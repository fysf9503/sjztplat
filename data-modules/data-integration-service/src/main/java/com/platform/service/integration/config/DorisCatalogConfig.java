package com.platform.service.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Doris Multi-Catalog 数据湖配置
 *
 * Doris 通过 Multi-Catalog 直接读取外部数据湖（Hudi/Iceberg/Hive），
 * 无需先将数据导入 Doris，支持联邦查询。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "doris.catalog")
public class DorisCatalogConfig {

    private String feHost = "localhost";
    private int feQueryPort = 9030;
    private String username = "root";
    private String password = "";

    private HiveCatalog hive = new HiveCatalog();
    private HudiCatalog hudi = new HudiCatalog();
    private IcebergCatalog iceberg = new IcebergCatalog();
    private MysqlCatalog mysql = new MysqlCatalog();
    private OceanBaseCatalog oceanbase = new OceanBaseCatalog();
    private GbaseCatalog gbase = new GbaseCatalog();
    private DamengCatalog dameng = new DamengCatalog();
    private KingbaseCatalog kingbase = new KingbaseCatalog();

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/", feHost, feQueryPort);
    }

    @Data
    public static class HiveCatalog {
        private String name = "hive_catalog";
        private String metastoreUris = "thrift://localhost:9083";
        private String hdfsUri = "hdfs://localhost:8020";
    }

    @Data
    public static class HudiCatalog {
        private String name = "hudi_catalog";
        private String metastoreUris = "thrift://localhost:9083";
        private String hdfsUri = "hdfs://localhost:8020";
    }

    @Data
    public static class IcebergCatalog {
        private String name = "iceberg_catalog";
        private String catalogType = "hive";
        private String warehouse = "hdfs://localhost:8020/warehouse/iceberg";
    }

    @Data
    public static class MysqlCatalog {
        private String name = "mysql_catalog";
        private String host = "localhost";
        private int port = 3306;
        private String username = "root";
        private String password = "";
    }

    @Data
    public static class OceanBaseCatalog {
        private String name = "oceanbase_catalog";
        private String host = "localhost";
        private int port = 2883;
        private String username = "root";
        private String password = "";
        private String database = "test";
    }

    @Data
    public static class GbaseCatalog {
        private String name = "gbase_catalog";
        private String host = "localhost";
        private int port = 5258;
        private String username = "gbasedbt";
        private String password = "";
        private String database = "testdb";
        private String serverName = "gbase01";
    }

    @Data
    public static class DamengCatalog {
        private String name = "dameng_catalog";
        private String host = "localhost";
        private int port = 5236;
        private String username = "SYSDBA";
        private String password = "";
        private String database = "DAMENG";
    }

    @Data
    public static class KingbaseCatalog {
        private String name = "kingbase_catalog";
        private String host = "localhost";
        private int port = 54321;
        private String username = "system";
        private String password = "";
        private String database = "test";
    }
}
