package net.hasor.dataql.rd.domain;

import com.jhr.datasource.operation.api.domain.api.SummonerApi;
import com.jhr.datasource.operation.api.domain.dto.DatasourceConnectionInfo;

/**
 * @author xukun
 * @since 1.0
 */
public class DatasourceOperation {
    private SummonerApi summonerApi;
    private DatasourceConnectionInfo datasourceConnectionInfo;

    public DatasourceOperation() {
    }

    public DatasourceOperation(SummonerApi summonerApi, DatasourceConnectionInfo datasourceConnectionInfo) {
        this.summonerApi = summonerApi;
        this.datasourceConnectionInfo = datasourceConnectionInfo;
    }

    public SummonerApi getSummonerApi() {
        return summonerApi;
    }

    public void setSummonerApi(SummonerApi summonerApi) {
        this.summonerApi = summonerApi;
    }

    public DatasourceConnectionInfo getDatasourceConnectionInfo() {
        return datasourceConnectionInfo;
    }

    public void setDatasourceConnectionInfo(DatasourceConnectionInfo datasourceConnectionInfo) {
        this.datasourceConnectionInfo = datasourceConnectionInfo;
    }
}
