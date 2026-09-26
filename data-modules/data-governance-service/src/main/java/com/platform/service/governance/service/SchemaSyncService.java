package com.platform.service.governance.service;

import com.platform.service.governance.dto.ColumnInfo;
import com.platform.service.governance.dto.SchemaDiffResult;
import com.platform.service.governance.entity.MetaTableSchema;
import com.platform.service.governance.entity.SchemaDiffLog;
import com.platform.service.governance.mapper.MetaTableSchemaMapper;
import com.platform.service.governance.mapper.SchemaDiffLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 表结构对比服务
 * 利用 Doris 原生 REFRESH CATALOG 刷新元数据，再通过 INFORMATION_SCHEMA.COLUMNS 对比
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaSyncService {

    private final JdbcTemplate dorisJdbcTemplate;
    private final MetaTableSchemaMapper schemaMapper;
    private final SchemaDiffLogMapper diffLogMapper;

    /**
     * 刷新 Catalog 元数据（调用 Doris 原生命令）
     */
    public void refreshCatalog(String catalogName) {
        String sql = String.format("REFRESH CATALOG %s", catalogName);
        try {
            dorisJdbcTemplate.execute(sql);
            log.info("Catalog 元数据刷新成功: {}", catalogName);
        } catch (Exception e) {
            log.warn("Catalog 刷新失败（可能不支持自动刷新）: {} - {}", catalogName, e.getMessage());
        }
    }

    /**
     * 获取指定表的列信息（通过 INFORMATION_SCHEMA）
     * 支持外部 Catalog 表和 Doris 内表
     */
    public List<ColumnInfo> getTableColumns(String catalog, String database, String table) {
        String fullTable = buildFullTableName(catalog, database, table);
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_COMMENT, IS_NULLABLE, COLUMN_DEFAULT, ORDINAL_POSITION " +
                     "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";

        String schema = (catalog != null && !catalog.isBlank()) ? database : database;
        if (catalog != null && !catalog.isBlank()) {
            schema = catalog + "." + database;
        }

        List<Map<String, Object>> rows = dorisJdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_COMMENT, IS_NULLABLE, COLUMN_DEFAULT, ORDINAL_POSITION " +
                "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                database, table);

        List<ColumnInfo> columns = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ColumnInfo col = new ColumnInfo();
            col.setName(getString(row, "COLUMN_NAME"));
            col.setType(getString(row, "COLUMN_TYPE") != null ? getString(row, "COLUMN_TYPE") : getString(row, "DATA_TYPE"));
            col.setComment(getString(row, "COLUMN_COMMENT"));
            col.setNullable(getString(row, "IS_NULLABLE"));
            col.setDefaultValue(getString(row, "COLUMN_DEFAULT"));
            col.setOrdinal(getInt(row, "ORDINAL_POSITION"));
            columns.add(col);
        }
        return columns;
    }

    /**
     * 对比源表和目标表结构
     */
    public SchemaDiffResult compareSchema(String catalog, String sourceDatabase, String sourceTable,
                                          String targetDatabase, String targetTable) {
        refreshCatalog(catalog);

        List<ColumnInfo> sourceCols = getTableColumns(catalog, sourceDatabase, sourceTable);
        List<ColumnInfo> targetCols = getTableColumns(null, targetDatabase, targetTable);

        SchemaDiffResult result = new SchemaDiffResult();
        result.setSourceColumns(sourceCols);
        result.setTargetColumns(targetCols);
        result.setSourceTable(buildFullTableName(catalog, sourceDatabase, sourceTable));
        result.setTargetTable(targetDatabase + "." + targetTable);

        Map<String, ColumnInfo> sourceMap = sourceCols.stream()
                .collect(Collectors.toMap(ColumnInfo::getName, c -> c, (a, b) -> a));
        Map<String, ColumnInfo> targetMap = targetCols.stream()
                .collect(Collectors.toMap(ColumnInfo::getName, c -> c, (a, b) -> a));

        for (String colName : sourceMap.keySet()) {
            if (!targetMap.containsKey(colName)) {
                SchemaDiffResult.DiffItem diff = new SchemaDiffResult.DiffItem("ADD", colName);
                diff.setSourceType(sourceMap.get(colName).getType());
                diff.setSourceComment(sourceMap.get(colName).getComment());
                diff.setDetail(String.format("源端新增字段: %s %s", colName, sourceMap.get(colName).getType()));
                result.getDiffs().add(diff);
            } else {
                ColumnInfo src = sourceMap.get(colName);
                ColumnInfo tgt = targetMap.get(colName);
                if (!Objects.equals(normalizeType(src.getType()), normalizeType(tgt.getType()))) {
                    SchemaDiffResult.DiffItem diff = new SchemaDiffResult.DiffItem("MODIFY", colName);
                    diff.setSourceType(src.getType());
                    diff.setTargetType(tgt.getType());
                    diff.setSourceComment(src.getComment());
                    diff.setTargetComment(tgt.getComment());
                    diff.setDetail(String.format("类型变更: %s → %s", tgt.getType(), src.getType()));
                    result.getDiffs().add(diff);
                }
            }
        }

        for (String colName : targetMap.keySet()) {
            if (!sourceMap.containsKey(colName)) {
                SchemaDiffResult.DiffItem diff = new SchemaDiffResult.DiffItem("DELETE", colName);
                diff.setTargetType(targetMap.get(colName).getType());
                diff.setTargetComment(targetMap.get(colName).getComment());
                diff.setDetail(String.format("源端已删除字段: %s %s", colName, targetMap.get(colName).getType()));
                result.getDiffs().add(diff);
            }
        }

        result.setHasDiff(!result.getDiffs().isEmpty());
        return result;
    }

    /**
     * 对已注册的表执行结构对比，并记录变更日志
     */
    @Transactional
    public SchemaDiffResult compareRegisteredTable(String schemaId) {
        MetaTableSchema meta = schemaMapper.findById(schemaId).orElse(null);
        if (meta == null) {
            throw new RuntimeException("表结构配置不存在: " + schemaId);
        }

        SchemaDiffResult result = compareSchema(
                meta.getSourceCatalog(), meta.getSourceDatabase(), meta.getSourceTable(),
                meta.getTargetDatabase(), meta.getTargetTable());

        if (result.isHasDiff()) {
            for (SchemaDiffResult.DiffItem diff : result.getDiffs()) {
                SchemaDiffLog logEntry = new SchemaDiffLog();
                logEntry.setTableSchemaId(schemaId);
                logEntry.setDiffType(diff.getType());
                logEntry.setColumnName(diff.getColumnName());
                logEntry.setOldType(diff.getTargetType());
                logEntry.setNewType(diff.getSourceType());
                logEntry.setOldComment(diff.getTargetComment());
                logEntry.setNewComment(diff.getSourceComment());
                logEntry.setDiffDetail(diff.getDetail());
                logEntry.setResolved("0");
                logEntry.setCreateTime(LocalDateTime.now());
                diffLogMapper.save(logEntry);
            }
            log.info("表结构变更: {} → {} ({}处差异)", result.getSourceTable(), result.getTargetTable(),
                    result.getDiffs().size());
        }

        meta.setLastSyncTime(LocalDateTime.now());
        meta.setUpdateTime(LocalDateTime.now());
        schemaMapper.save(meta);

        return result;
    }

    /**
     * 对所有已注册的表执行批量对比
     */
    public List<SchemaDiffResult> compareAll(String groupId) {
        List<MetaTableSchema> tables;
        if (groupId != null && !groupId.isBlank()) {
            tables = schemaMapper.findByStatusAndGroupId("1", groupId);
        } else {
            tables = schemaMapper.findByStatus("1");
        }

        List<SchemaDiffResult> results = new ArrayList<>();

        for (MetaTableSchema meta : tables) {
            try {
                SchemaDiffResult result = compareRegisteredTable(meta.getId());
                result.setTableSchemaId(meta.getId());
                results.add(result);
            } catch (Exception e) {
                log.error("表结构对比失败: {}.{}", meta.getSourceDatabase(), meta.getSourceTable(), e);
            }
        }

        return results;
    }

    public List<MetaTableSchema> listRegisteredTables(String groupId) {
        if (groupId != null && !groupId.isBlank()) {
            return schemaMapper.findByGroupId(groupId);
        }
        return schemaMapper.findAll();
    }

    public MetaTableSchema registerTable(MetaTableSchema meta) {
        meta.setStatus("1");
        meta.setCreateTime(LocalDateTime.now());
        meta.setUpdateTime(LocalDateTime.now());
        schemaMapper.save(meta);
        return meta;
    }

    public void unregisterTable(String id) {
        schemaMapper.deleteById(id);
        diffLogMapper.deleteByTableSchemaId(id);
    }

    public List<SchemaDiffLog> getDiffLogs(String schemaId, String resolved) {
        if (resolved != null && !resolved.isBlank()) {
            return diffLogMapper.findByTableSchemaIdAndResolvedOrderByCreateTimeDesc(schemaId, resolved);
        }
        return diffLogMapper.findByTableSchemaIdOrderByCreateTimeDesc(schemaId);
    }

    @Transactional
    public void resolveDiff(String diffId, String action, String userId) {
        SchemaDiffLog log = diffLogMapper.findById(diffId).orElse(null);
        if (log == null) return;

        if ("apply".equals(action)) {
            applySchemaDiff(log);
        }

        log.setResolved("1".equals(action) || "apply".equals(action) ? "1" : "2");
        log.setResolvedBy(userId);
        log.setResolvedTime(LocalDateTime.now());
        diffLogMapper.save(log);
    }

    private void applySchemaDiff(SchemaDiffLog diff) {
        MetaTableSchema meta = schemaMapper.findById(diff.getTableSchemaId()).orElse(null);
        if (meta == null) return;

        String targetTable = meta.getTargetDatabase() + "." + meta.getTargetTable();

        switch (diff.getDiffType()) {
            case "ADD" -> {
                String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s",
                        targetTable, diff.getColumnName(),
                        diff.getNewType() != null ? diff.getNewType() : "VARCHAR(256)");
                dorisJdbcTemplate.execute(sql);
                log.info("已应用DDL变更(ADD): {}", sql);
            }
            case "MODIFY" -> {
                String sql = String.format("ALTER TABLE %s MODIFY COLUMN %s %s",
                        targetTable, diff.getColumnName(),
                        diff.getNewType() != null ? diff.getNewType() : diff.getOldType());
                dorisJdbcTemplate.execute(sql);
                log.info("已应用DDL变更(MODIFY): {}", sql);
            }
            case "DELETE" -> {
                log.info("字段删除需人工确认，跳过自动执行: {}.{}", targetTable, diff.getColumnName());
            }
        }
    }

    private String normalizeType(String type) {
        if (type == null) return "";
        return type.toLowerCase().replaceAll("\\s+", "").replaceAll("\\(.*\\)", "");
    }

    private String buildFullTableName(String catalog, String database, String table) {
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

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0;
        if (val instanceof Number num) return num.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }
}
