package com.platform.service.governance.service;

import com.platform.service.governance.dto.ColumnInfo;
import com.platform.service.governance.parser.ParamParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 自动建表服务
 * 查询源表结构 → 生成 Doris DDL（含注释、分区）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDdlService {

    private final SchemaSyncService schemaSyncService;
    private final JdbcTemplate dorisJdbcTemplate;
    private final ParamParser paramParser;

    /**
     * 生成建表 DDL
     */
    public String generateDdl(String catalog, String database, String table,
                              String targetDatabase, String targetTable,
                              String partitionField, String partitionType,
                              String engine, String uniqueKey,
                              boolean includeComments) {

        schemaSyncService.refreshCatalog(catalog);
        List<ColumnInfo> columns = schemaSyncService.getTableColumns(catalog, database, table);

        if (columns.isEmpty()) {
            throw new RuntimeException("未获取到源表字段: " + buildFullName(catalog, database, table));
        }

        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(targetDatabase).append(".").append(targetTable).append(" (\n");

        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            ddl.append("    ").append(col.getName()).append(" ").append(convertType(col.getType()));

            if ("NO".equalsIgnoreCase(col.getNullable())) {
                ddl.append(" NOT NULL");
            } else {
                ddl.append(" NULL");
            }

            if (includeComments && col.getComment() != null && !col.getComment().isEmpty()) {
                String comment = col.getComment().replace("'", "\\'");
                ddl.append(" COMMENT '").append(comment).append("'");
            }

            if (i < columns.size() - 1) {
                ddl.append(",");
            }
            ddl.append("\n");
        }

        ddl.append(")\n");

        if (engine == null || engine.isBlank()) {
            engine = "OLAP";
        }
        ddl.append("ENGINE = ").append(engine).append("\n");

        if (uniqueKey != null && !uniqueKey.isBlank()) {
            ddl.append("UNIQUE KEY(").append(uniqueKey).append(")\n");
        }

        if (partitionField != null && !partitionField.isBlank()) {
            String resolvedField = paramParser.parse(partitionField);
            if ("RANGE".equalsIgnoreCase(partitionType)) {
                ddl.append("PARTITION BY RANGE(").append(resolvedField).append(")()\n");
            } else {
                ddl.append("PARTITION BY ").append(resolvedField).append("\n");
            }
        }

        ddl.append("DISTRIBUTED BY HASH(").append(columns.get(0).getName()).append(") BUCKETS 10\n");
        ddl.append("PROPERTIES(\n");
        ddl.append("    \"replication_allocation\" = \"tag.location.default: 1\"");
        if (partitionField != null && !partitionField.isBlank()) {
            ddl.append(",\n    \"dynamic_partition.enable\" = \"true\"");
            ddl.append(",\n    \"dynamic_partition.time_unit\" = \"DAY\"");
            ddl.append(",\n    \"dynamic_partition.start\" = \"-365\"");
            ddl.append(",\n    \"dynamic_partition.end\" = \"3\"");
            ddl.append(",\n    \"dynamic_partition.prefix\" = \"p\"");
            ddl.append(",\n    \"dynamic_partition.buckets\" = \"10\"");
        }
        ddl.append("\n);");

        return ddl.toString();
    }

    /**
     * 执行建表 DDL
     */
    public void executeDdl(String ddl) {
        dorisJdbcTemplate.execute(ddl);
        log.info("建表成功: {}", ddl.substring(0, Math.min(ddl.length(), 80)));
    }

    /**
     * 生成 CREATE TABLE AS SELECT 语句（Doris 原生支持）
     */
    public String generateCtas(String catalog, String database, String table,
                              String targetDatabase, String targetTable,
                              String whereClause) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(targetDatabase).append(".").append(targetTable).append("\n");
        sql.append("PROPERTIES(\"replication_allocation\" = \"tag.location.default: 1\")\n");
        sql.append("AS SELECT * FROM ").append(buildFullName(catalog, database, table));

        String resolved = paramParser.parse(whereClause);
        if (resolved != null && !resolved.isBlank()) {
            sql.append(" WHERE ").append(resolved);
        }

        return sql.toString();
    }

    /**
     * 获取源表预览数据（前10行）
     */
    public List<Map<String, Object>> previewData(String catalog, String database, String table) {
        String sql = String.format("SELECT * FROM %s LIMIT 10", buildFullName(catalog, database, table));
        return dorisJdbcTemplate.queryForList(sql);
    }

    /**
     * 源端类型转 Doris 类型
     */
    private String convertType(String sourceType) {
        if (sourceType == null) return "VARCHAR(256)";
        String t = sourceType.toLowerCase().trim();

        // 通用整型
        if (t.startsWith("tinyint")) return "TINYINT";
        if (t.startsWith("smallint")) return "SMALLINT";
        if (t.startsWith("mediumint")) return "INT";
        if (t.startsWith("int") || t.startsWith("integer")) return "INT";
        if (t.startsWith("bigint")) return "BIGINT";

        // OceanBase / MySQL 特有
        if (t.startsWith("float")) return "FLOAT";
        if (t.startsWith("double")) return "DOUBLE";
        if (t.startsWith("decimal") || t.startsWith("numeric")) return t.toUpperCase();

        // 达梦 DM 特有类型
        if (t.startsWith("number")) return "DECIMAL(18,2)";
        if (t.startsWith("int1") || t.startsWith("int2")) return "SMALLINT";
        if (t.startsWith("int8") || t.startsWith("int4")) return "INT";
        if (t.startsWith("bigint") || t.startsWith("int8")) return "BIGINT";
        if (t.startsWith("real")) return "DOUBLE";
        if (t.startsWith("binary_float")) return "FLOAT";
        if (t.startsWith("binary_double")) return "DOUBLE";
        if (t.startsWith("raw") || t.startsWith("longraw")) return "STRING";
        if (t.startsWith("blob") || t.startsWith("clob") || t.startsWith("nclob")) return "STRING";
        if (t.startsWith("long")) return "STRING";
        if (t.startsWith("rowid") || t.startsWith("urowid")) return "VARCHAR(64)";

        // 人大金仓 KingbaseES 特有（PostgreSQL 兼容）
        if (t.startsWith("serial")) return "INT";
        if (t.startsWith("bigserial")) return "BIGINT";
        if (t.startsWith("smallserial")) return "SMALLINT";
        if (t.startsWith("bytea")) return "STRING";
        if (t.startsWith("uuid")) return "VARCHAR(36)";
        if (t.startsWith("inet") || t.startsWith("cidr")) return "VARCHAR(43)";
        if (t.startsWith("macaddr")) return "VARCHAR(17)";
        if (t.startsWith("tsvector")) return "STRING";

        // GBase 8s 特有（Informix 兼容）
        if (t.startsWith("serial") || t.startsWith("serial8")) return t.startsWith("serial8") ? "BIGINT" : "INT";
        if (t.startsWith("smartserial")) return "BIGINT";
        if (t.startsWith("int8")) return "BIGINT";
        if (t.startsWith("money")) return "DECIMAL(18,2)";
        if (t.startsWith("interval")) return "VARCHAR(32)";
        if (t.startsWith("lvarchar")) return "VARCHAR(1024)";
        if (t.startsWith("list") || t.startsWith("set") || t.startsWith("multiset")) return "STRING";

        // 日期时间类型
        if (t.startsWith("date")) return "DATE";
        if (t.startsWith("datetime") || t.startsWith("timestamp")) return "DATETIME";
        if (t.startsWith("time")) return "VARCHAR(8)";

        // 布尔
        if (t.startsWith("boolean") || t.startsWith("bit")) return "BOOLEAN";
        if (t.startsWith("bool")) return "BOOLEAN";

        // 字符类型
        if (t.startsWith("varchar")) return t.toUpperCase();
        if (t.startsWith("char")) return t.toUpperCase();
        if (t.startsWith("nchar") || t.startsWith("nvarchar")) return "STRING";
        if (t.startsWith("text") || t.startsWith("string")) return "STRING";

        // JSON
        if (t.startsWith("json")) return "JSON";

        return "VARCHAR(256)";
    }

    private String buildFullName(String catalog, String database, String table) {
        StringBuilder sb = new StringBuilder();
        if (catalog != null && !catalog.isBlank()) {
            sb.append(catalog).append(".");
        }
        if (database != null && !database.isBlank()) {
            sb.append(database).append(".");
        }
        sb.append(table);
        return sb.toString();
    }
}
