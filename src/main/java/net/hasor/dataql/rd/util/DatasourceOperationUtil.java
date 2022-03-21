package net.hasor.dataql.rd.util;

import com.jhr.datasource.operation.api.domain.response.DsResponseResult;

/**
 * @author xukun
 * @since 1.0
 */
public class DatasourceOperationUtil {
    public static <T> T getResponseData(DsResponseResult<T> responseResult) {
        if (!responseResult.isSuccess()) {
            throw new RuntimeException("ExecuteSQL Failed: " + responseResult.getMessage());
        }
        return responseResult.getData();
    }
}
