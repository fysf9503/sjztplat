package com.platform.service.integration.service;

import com.platform.service.integration.config.DorisCatalogConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 数据湖联邦查询服务
 *
 * 利用 Doris Multi-Catalog 能力，直接查询外部数据湖中的数据：
 * - Hive：通过 Hive Metastore 访问 Hive 表
 * - Hudi：通过 Hive Metastore + HDFS 访问 Hudi 表
 * - Iceberg：通过 Hive Metastore 或 REST Catalog 访问 Iceberg 表
 * - MySQL：通过 JDBC Catalog 直接查询 MySQL 表
 *
 * 所有外部数据源注册为 Doris Catalog 后，可以用标准 SQL 跨源查询：
 *   SELECT * FROM hudi_catalog.db.table
 *   SELECT * FROM iceberg_catalog.db.table
 *   SELECT * FROM mysql_catalog.db.table
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataLakeService {

    private final DataSource dataSource;
    private final DorisCatalogConfig catalogConfig;

    /**
     * 创建 Hudi Catalog
     */
    public String createHudiCatalog() {
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'hudi',
                    'hive.metastore.uris' = '%s',
                    'hadoop.username' = 'hdfs'
                )
                """, catalogConfig.getHudi().getName(),
                catalogConfig.getHudi().getMetastoreUris());

        return executeCatalogDdl(sql, catalogConfig.getHudi().getName());
    }

    /**
     * 创建 Iceberg Catalog
     */
    public String createIcebergCatalog() {
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'iceberg',
                    'iceberg.catalog.type' = '%s',
                    'warehouse' = '%s'
                )
                """, catalogConfig.getIceberg().getName(),
                catalogConfig.getIceberg().getCatalogType(),
                catalogConfig.getIceberg().getWarehouse());

        return executeCatalogDdl(sql, catalogConfig.getIceberg().getName());
    }

    /**
     * 创建 Hive Catalog
     */
    public String createHiveCatalog() {
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'hive',
                    'hive.metastore.uris' = '%s',
                    'hadoop.username' = 'hdfs'
                )
                """, catalogConfig.getHive().getName(),
                catalogConfig.getHive().getMetastoreUris());

        return executeCatalogDdl(sql, catalogConfig.getHive().getName());
    }

    /**
     * 创建 MySQL Catalog（JDBC Catalog）
     */
    public String createMysqlCatalog() {
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'jdbc',
                    'user' = '%s',
                    'password' = '%s',
                    'jdbc_url' = 'jdbc:mysql://%s:%d',
                    'driver_url' = 'mysql-connector-j-8.0.33.jar',
                    'driver_class' = 'com.mysql.cj.jdbc.Driver'
                )
                """, catalogConfig.getMysql().getName(),
                catalogConfig.getMysql().getUsername(),
                catalogConfig.getMysql().getPassword(),
                catalogConfig.getMysql().getHost(),
                catalogConfig.getMysql().getPort());

        return executeCatalogDdl(sql, catalogConfig.getMysql().getName());
    }

    /**
     * 创建 OceanBase Catalog（JDBC Catalog）
     * OceanBase 兼容 MySQL 协议，使用 OceanBase JDBC 驱动
     */
    public String createOceanBaseCatalog() {
        var cfg = catalogConfig.getOceanbase();
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'jdbc',
                    'user' = '%s',
                    'password' = '%s',
                    'jdbc_url' = 'jdbc:oceanbase://%s:%d/%s',
                    'driver_url' = 'oceanbase-client-2.4.12.jar',
                    'driver_class' = 'com.oceanbase.jdbc.Driver'
                )
                """, cfg.getName(), cfg.getUsername(), cfg.getPassword(),
                cfg.getHost(), cfg.getPort(), cfg.getDatabase());

        return executeCatalogDdl(sql, cfg.getName());
    }

    /**
     * 创建 GBase 8s Catalog（JDBC Catalog）
     * GBase 8s 使用 gbasedbt 驱动
     */
    public String createGbaseCatalog() {
        var cfg = catalogConfig.getGbase();
        String jdbcUrl = String.format(
                "jdbc:gbasedbt-sqli://%s:%d/%s:GBASEDBTSERVER=%s;DB_LOCALE=zh_CN.utf8;CLIENT_LOCALE=zh_CN.utf8",
                cfg.getHost(), cfg.getPort(), cfg.getDatabase(), cfg.getServerName());
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'jdbc',
                    'user' = '%s',
                    'password' = '%s',
                    'jdbc_url' = '%s',
                    'driver_url' = 'gbasedbt-jdbc-driver.jar',
                    'driver_class' = 'com.gbasedbt.jdbc.Driver'
                )
                """, cfg.getName(), cfg.getUsername(), cfg.getPassword(), jdbcUrl);

        return executeCatalogDdl(sql, cfg.getName());
    }

    /**
     * 创建 达梦 DM Catalog（JDBC Catalog）
     */
    public String createDamengCatalog() {
        var cfg = catalogConfig.getDameng();
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'jdbc',
                    'user' = '%s',
                    'password' = '%s',
                    'jdbc_url' = 'jdbc:dm://%s:%d/%s',
                    'driver_url' = 'DmJdbcDriver18.jar',
                    'driver_class' = 'dm.jdbc.driver.DmDriver'
                )
                """, cfg.getName(), cfg.getUsername(), cfg.getPassword(),
                cfg.getHost(), cfg.getPort(), cfg.getDatabase());

        return executeCatalogDdl(sql, cfg.getName());
    }

    /**
     * 创建 人大金仓 KingbaseES Catalog（JDBC Catalog）
     */
    public String createKingbaseCatalog() {
        var cfg = catalogConfig.getKingbase();
        String sql = String.format("""
                CREATE CATALOG IF NOT EXISTS %s PROPERTIES (
                    'type' = 'jdbc',
                    'user' = '%s',
                    'password' = '%s',
                    'jdbc_url' = 'jdbc:kingbase8://%s:%d/%s',
                    'driver_url' = 'kingbase8-8.6.0.jar',
                    'driver_class' = 'com.kingbase8.Driver'
                )
                """, cfg.getName(), cfg.getUsername(), cfg.getPassword(),
                cfg.getHost(), cfg.getPort(), cfg.getDatabase());

        return executeCatalogDdl(sql, cfg.getName());
    }

    /**
     * 联邦查询：从外部数据湖查数据
     * 示例：SELECT * FROM hudi_catalog.my_db.my_table LIMIT 100
     */
    public List<Map<String, Object>> query(String sql) {
        log.info("联邦查询: {}", sql);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        return jdbc.queryForList(sql);
    }

    /**
     * 通过 INSERT INTO SELECT 从外部数据湖抽取数据到 Doris 内表
     * 示例：INSERT INTO doris_table SELECT * FROM hudi_catalog.db.hudi_table
     */
    public int extractFromDataLake(String dorisTable, String sourceCatalogTable, String condition) {
        String where = (condition != null && !condition.isBlank()) ? " WHERE " + condition : "";
        String sql = String.format("INSERT INTO %s SELECT * FROM %s%s", dorisTable, sourceCatalogTable, where);
        log.info("数据湖抽取: {}", sql);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        return jdbc.update(sql);
    }

    /**
     * 创建 Doris 物化视图（定时刷新外部数据到 Doris）
     */
    public String createSyncedMaterializedView(String mvName, String sourceQuery, String refreshInterval) {
        String sql = String.format("""
                CREATE MATERIALIZED VIEW %s
                BUILD IMMEDIATE REFRESH AUTO ON SCHEDULE EVERY %s
                DISTRIBUTED BY HASH(id) BUCKETS 3
                PROPERTIES('replication_allocation' = 'tag.location.default: 1')
                AS %s
                """, mvName, refreshInterval, sourceQuery);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(sql);
        return "物化视图 " + mvName + " 创建成功";
    }

    private String executeCatalogDdl(String sql, String catalogName) {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute(sql);
            log.info("Catalog 创建成功: {}", catalogName);
            return "Catalog [" + catalogName + "] 创建成功";
        } catch (Exception e) {
            log.error("Catalog 创建失败: {}, 错误: {}", catalogName, e.getMessage());
            return "Catalog [" + catalogName + "] 创建失败: " + e.getMessage();
        }
    }
}
