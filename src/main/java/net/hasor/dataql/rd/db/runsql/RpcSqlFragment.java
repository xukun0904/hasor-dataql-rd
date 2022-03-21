package net.hasor.dataql.rd.db.runsql;

import com.jhr.datasource.operation.api.domain.api.SummonerApi;
import com.jhr.datasource.operation.api.domain.dto.DatasourceConnectionInfo;
import net.hasor.core.BindInfo;
import net.hasor.core.Singleton;
import net.hasor.dataql.Hints;
import net.hasor.dataql.fx.db.FxSqlCheckChainSpi;
import net.hasor.dataql.fx.db.runsql.SqlFragment;
import net.hasor.dataql.rd.db.RpcLookupDataSourceListener;
import net.hasor.dataql.rd.db.dialect.CustomHiveDialect;
import net.hasor.dataql.rd.domain.Constants;
import net.hasor.dataql.rd.domain.DatasourceOperation;
import net.hasor.dataql.rd.util.DatasourceOperationUtil;
import net.hasor.db.dal.fxquery.FxQuery;
import net.hasor.db.dialect.BoundSql;
import net.hasor.db.dialect.SqlDialect;
import net.hasor.db.dialect.SqlDialectRegister;
import net.hasor.utils.StringUtils;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.hasor.dataql.fx.FxHintNames.FRAGMENT_SQL_DATA_SOURCE;
import static net.hasor.dataql.fx.FxHintNames.FRAGMENT_SQL_PAGE_DIALECT;

/**
 * 修改底层为远程调用数据源
 *
 * @author xk
 * @version : 2021-08-20
 */
@Singleton
public class RpcSqlFragment extends SqlFragment {

    private Map<String, DatasourceOperation> dataSourceMap;

    @Override
    @PostConstruct
    public void init() {
        this.dataSourceMap = new HashMap<>();
        List<BindInfo<DatasourceOperation>> bindInfos = this.appContext.findBindingRegister(DatasourceOperation.class);
        for (BindInfo<DatasourceOperation> bindInfo : bindInfos) {
            DatasourceOperation dataSource = this.appContext.getInstance(bindInfo);
            if (dataSource != null) {
                this.dataSourceMap.put(bindInfo.getBindName(), dataSource);
            }
        }
    }

    private DatasourceOperation getDatasourceOperation(String sourceName) {
        // .其次在通过数据源获取
        DatasourceOperation useDataSource = this.dataSourceMap.get(sourceName);
        if (useDataSource == null) {
            if (this.spiTrigger.hasSpi(RpcLookupDataSourceListener.class)) {
                // .通过 SPI 查找数据源
                DatasourceOperation dataSource = this.spiTrigger.notifySpi(RpcLookupDataSourceListener.class, (listener, lastResult) -> listener.lookUp(sourceName), null);
                if (dataSource != null) {
                    return dataSource;
                }
            }
        }
        throw new NullPointerException("DataSource " + sourceName + " is undefined.");
    }

    @Override
    public List<Object> batchRunFragment(Hints hint, List<Map<String, Object>> params, String fragmentString) throws Throwable {
        // 如果批量参数为空退：退化为 非批量
        if (params == null || params.size() == 0) {
            return Collections.singletonList(this.runFragment(hint, Collections.emptyMap(), fragmentString));
        }
        // 批量参数只有一组：退化为 非批量
        if (params.size() == 1) {
            return Collections.singletonList(this.runFragment(hint, params.get(0), fragmentString));
        }
        // 有占位符 或者 Insert/Update/Delete 之外的
        FxQuery fxSql = analysisSQL(hint, fragmentString);
        // 非批量模式
        List<Object> resultList = new ArrayList<>(params.size());
        for (Map<String, Object> paramItem : params) {
            if (usePage(hint)) {
                resultList.add(this.usePageFragment(fxSql, hint, paramItem));
            } else {
                resultList.add(this.noPageFragment(fxSql, hint, paramItem));
            }
        }
        return resultList;
    }

    /**
     * 分页模式
     */
    @Override
    protected Object usePageFragment(FxQuery fxSql, Hints hints, Map<String, Object> paramMap) {
        // 优先从 hint 中取方言，取不到在自动推断
        String sqlDialect = hints.getOrDefault(FRAGMENT_SQL_PAGE_DIALECT.name(), "").toString();
        if (StringUtils.isBlank(sqlDialect)) {
            String useDataSource = hints.getOrDefault(FRAGMENT_SQL_DATA_SOURCE.name(), "").toString();
            DatasourceOperation datasourceOperation = getDatasourceOperation(useDataSource);
            DatasourceConnectionInfo connectionInfo = datasourceOperation.getDatasourceConnectionInfo();
            sqlDialect = Constants.SQL_DIALECT_MAP.get(connectionInfo.getDsourceType());
            if (StringUtils.isBlank(sqlDialect)) {
                throw new IllegalArgumentException("Query dialect missing.");
            }
        }
        SqlDialectRegister.registerDialectAlias("hive", CustomHiveDialect.class);
        final SqlDialect pageDialect = SqlDialectRegister.findOrCreate(sqlDialect, this.appContext);
        // Hints hints,查询包含的 Hint
        // FxQuery fxQuery, 查询语句
        // Map<String, Object> queryParamMap, 查询参数
        // SqlPageDialect pageDialect, 分页方言服务
        // SqlPageQuery pageQuery, 用于执行分页查询的服务
        String sqlQuery = fxSql.buildQueryString(paramMap);
        Object[] sqlArgs = fxSql.buildParameterSource(paramMap).toArray();
        BoundSql boundSql = new BoundSql.BoundSqlObj(sqlQuery, sqlArgs);
        return new RpcSqlPageObject(hints, boundSql, pageDialect, this);
    }

    /**
     * 非分页模式
     */
    @Override
    protected Object noPageFragment(FxQuery fxSql, Hints hint, Map<String, Object> paramMap) throws Throwable {
        // 获取必要的参数
        String useSourceName = hint.getOrDefault(FRAGMENT_SQL_DATA_SOURCE.name(), "").toString();
        String buildQueryString = fxSql.buildQueryString(paramMap);
        Object[] buildQueryParams = fxSql.buildParameterSource(paramMap).toArray();
        return this.executeSQL(useSourceName, buildQueryString, buildQueryParams, (queryString, queryParams, useDataSource) -> {
            // 执行查询
            SummonerApi summonerApi = useDataSource.getSummonerApi();
            DatasourceConnectionInfo connectionInfo = useDataSource.getDatasourceConnectionInfo();
            connectionInfo.setSqlMap(Collections.singletonMap(buildQueryString, buildQueryParams));
            List<Map<String, Object>> resultDataSet = DatasourceOperationUtil.getResponseData(summonerApi.queryForList(connectionInfo));
            // 返回结果
            if (resultDataSet.size() <= 1) {
                return resultDataSet.get(0);
            } else {
                return resultDataSet;
            }
        });
    }

    <T> T executeSQL(String sourceName, String sqlString, Object[] paramArrays, RpcSqlQuery<T> sqlQuery) throws SQLException {
        return executeSQL(false, sourceName, sqlString, paramArrays, sqlQuery);
    }

    /**
     * 执行 SQL
     */
    protected <T> T executeSQL(boolean batch, String sourceName, String sqlString, Object[] paramArrays, RpcSqlQuery<T> sqlQuery) throws SQLException {
        if (this.spiTrigger.hasSpi(FxSqlCheckChainSpi.class)) {
            final FxSqlCheckChainSpi.FxSqlInfo fxSqlInfo = new FxSqlCheckChainSpi.FxSqlInfo(batch, sourceName, sqlString, paramArrays);
            final AtomicBoolean doExit = new AtomicBoolean(false);
            this.spiTrigger.chainSpi(FxSqlCheckChainSpi.class, (listener, lastResult) -> {
                if (doExit.get()) {
                    return lastResult;
                }
                int doCheck = listener.doCheck(fxSqlInfo);
                if (doCheck == FxSqlCheckChainSpi.EXIT) {
                    doExit.set(true);
                }
                return lastResult;
            }, fxSqlInfo);
            return sqlQuery.doQuery(fxSqlInfo.getQueryString(), fxSqlInfo.getQueryParams(), this.getDatasourceOperation(sourceName));
        } else {
            return sqlQuery.doQuery(sqlString, paramArrays, this.getDatasourceOperation(sourceName));
        }
    }

    public interface RpcSqlQuery<T> {
        /**
         * 执行 SQL
         */
        T doQuery(String querySQL, Object[] params, DatasourceOperation useDataSource) throws SQLException;
    }

    /**
     * 结果转换
     */
    @Override
    public Object convertResult(Hints hint, List<Map<String, Object>> mapList) {
        return super.convertResult(hint, mapList);
    }
}
