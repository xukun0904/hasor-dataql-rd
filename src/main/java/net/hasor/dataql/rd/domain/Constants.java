package net.hasor.dataql.rd.domain;

import net.hasor.db.JdbcUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xukun
 * @since 1.0
 */
public class Constants {

    public static final Short DATA_CONN_TYPE_GAUSSDB = 1;
    public static final Short DATA_CONN_TYPE_MYSQL = 2;
    public static final Short DATA_CONN_TYPE_ORACLE = 3;
    public static final Short DATA_CONN_TYPE_HIVE = 21;
    public static final Map<Short, String> SQL_DIALECT_MAP = new HashMap<>();

    static {
        SQL_DIALECT_MAP.put(DATA_CONN_TYPE_GAUSSDB, JdbcUtils.POSTGRESQL);
        SQL_DIALECT_MAP.put(DATA_CONN_TYPE_MYSQL, JdbcUtils.MYSQL);
        SQL_DIALECT_MAP.put(DATA_CONN_TYPE_ORACLE, JdbcUtils.ORACLE);
        SQL_DIALECT_MAP.put(DATA_CONN_TYPE_HIVE, JdbcUtils.HIVE);
    }
}
