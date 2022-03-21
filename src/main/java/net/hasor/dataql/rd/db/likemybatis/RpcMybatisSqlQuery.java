package net.hasor.dataql.rd.db.likemybatis;

import net.hasor.dataql.fx.db.likemybatis.SqlNode;
import net.hasor.db.dal.fxquery.DefaultFxQuery;

import java.util.List;
import java.util.Map;

/**
 * @author xukun
 * @version : 2021-08-20
 */
class RpcMybatisSqlQuery extends DefaultFxQuery {
    private SqlNode sqlNode;

    public RpcMybatisSqlQuery(SqlNode sqlNode) {
        this.sqlNode = sqlNode;
    }

    @Override
    public String buildQueryString(Object context) {
        if (context instanceof Map) {
            return sqlNode.getSql((Map<String, Object>) context);
        } else {
            throw new IllegalArgumentException("context must be instance of Map");
        }
    }

    @Override
    public List<Object> buildParameterSource(Object context) {
        return this.sqlNode.getParameters();
    }
}
