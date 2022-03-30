# Hasor-DataQL-RD数据查询引擎

对[dataql-dataway](https://github.com/ClouGence/hasor/tree/master/hasor-dataql/dataql-dataway)进行二次开发，修改底层查询方法，通过OpenFeign远程调用数据源服务进行查询，解决闭源JDBC数据源无法查询问题。