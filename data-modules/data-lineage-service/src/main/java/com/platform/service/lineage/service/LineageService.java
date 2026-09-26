package com.platform.service.lineage.service;

import com.platform.common.om.client.OmLineageClient;
import com.platform.common.om.client.OmTableClient;
import com.platform.service.lineage.entity.DataLineage;
import com.platform.service.lineage.entity.DataLineageField;
import com.platform.service.lineage.mapper.DataLineageFieldMapper;
import com.platform.service.lineage.mapper.DataLineageMapper;
import com.platform.service.lineage.parser.SqlLineageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据血缘服务
 * 本地使用 Druid SQL Parser 解析血缘关系，同时推送至 OpenMetadata 进行统一元数据管理
 * 查询时优先从 OpenMetadata 获取，降级时使用本地存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {

    private final DataLineageMapper lineageMapper;
    private final DataLineageFieldMapper fieldMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OmLineageClient omLineageClient;
    private final OmTableClient omTableClient;

    /**
     * 从 SQL 解析血缘并保存
     * 同时推送到 OpenMetadata
     */
    @Transactional
    public DataLineage saveLineageFromSql(String sql, String jobId, String jobType, String groupId) {
        SqlLineageParser parser = new SqlLineageParser();
        SqlLineageParser.ParseResult parseResult = parser.parse(sql);

        if (parseResult.tableLineages.isEmpty()) {
            log.warn("未解析到表级血缘: {}", sql.substring(0, Math.min(sql.length(), 80)));
            return null;
        }

        // 1. 本地保存
        SqlLineageParser.TableLineage firstTable = parseResult.tableLineages.get(0);
        DataLineage lineage = new DataLineage();
        lineage.setSourceCatalog(firstTable.getSourceCatalog());
        lineage.setSourceDatabase(firstTable.getSourceDatabase());
        lineage.setSourceTable(firstTable.getSourceTable());
        lineage.setTargetDatabase(firstTable.getTargetDatabase());
        lineage.setTargetTable(firstTable.getTargetTable());
        lineage.setTransformSql(sql);
        lineage.setJobId(jobId);
        lineage.setJobType(jobType);
        lineage.setGroupId(groupId);
        lineage.setCreateTime(LocalDateTime.now());
        lineageMapper.save(lineage);

        for (SqlLineageParser.FieldLineage fl : parseResult.fieldLineages) {
            if ("wildcard".equals(fl.getFieldType())) {
                expandWildcardFields(lineage.getId(), fl);
            } else {
                DataLineageField field = new DataLineageField();
                field.setLineageId(lineage.getId());
                field.setSourceDatabase(fl.getSourceDatabase());
                field.setSourceTable(fl.getSourceTable());
                field.setSourceField(fl.getSourceField());
                field.setTargetDatabase(fl.getTargetDatabase());
                field.setTargetTable(fl.getTargetTable());
                field.setTargetField(fl.getTargetField());
                field.setTransformExpr(fl.getTransformExpr());
                field.setFieldType(fl.getFieldType());
                fieldMapper.save(field);
            }
        }

        // 2. 推送到 OpenMetadata（降级容错，失败不影响本地保存）
        pushLineageToOpenMetadata(parseResult, sql);

        log.info("血缘保存成功: {} → {} ({}条字段映射), 已推送至OpenMetadata",
                lineage.getSourceTable(), lineage.getTargetTable(),
                parseResult.fieldLineages.size());
        return lineage;
    }

    /**
     * 将解析到的表级血缘推送到 OpenMetadata
     */
    private void pushLineageToOpenMetadata(SqlLineageParser.ParseResult parseResult, String sql) {
        for (SqlLineageParser.TableLineage tl : parseResult.tableLineages) {
            try {
                String fromFqn = buildFqn(tl.getSourceCatalog(), tl.getSourceDatabase(), tl.getSourceTable());
                String toFqn = buildFqn(null, tl.getTargetDatabase(), tl.getTargetTable());

                // 先尝试通过 FQN 获取表的 OM ID
                String fromId = getEntityIdFromOm("table", fromFqn);
                String toId = getEntityIdFromOm("table", toFqn);

                Map<String, Object> result = omLineageClient.addLineage(
                        fromId, "table", fromFqn,
                        toId, "table", toFqn,
                        sql);

                if (result != null) {
                    log.info("血缘已推送至OpenMetadata: {} → {}", fromFqn, toFqn);
                } else {
                    log.warn("推送血缘至OpenMetadata失败(降级): {} → {}", fromFqn, toFqn);
                }
            } catch (Exception e) {
                log.warn("推送血缘至OpenMetadata异常(降级): {}", e.getMessage());
            }
        }
    }

    /**
     * 通过 FQN 从 OpenMetadata 获取实体 ID
     */
    @SuppressWarnings("unchecked")
    private String getEntityIdFromOm(String entityType, String fqn) {
        try {
            Map<String, Object> table = omTableClient.getTableByFqn(fqn, "id");
            if (table != null && table.containsKey("id")) {
                Object id = table.get("id");
                return id != null ? id.toString() : null;
            }
        } catch (Exception e) {
            log.debug("从OM获取实体ID失败: {}", e.getMessage());
        }
        return null;
    }

    private String buildFqn(String catalog, String database, String table) {
        StringBuilder fqn = new StringBuilder();
        if (catalog != null && !catalog.isBlank()) fqn.append(catalog).append(".");
        if (database != null && !database.isBlank()) fqn.append(database).append(".");
        fqn.append(table);
        return fqn.toString();
    }

    private void expandWildcardFields(String lineageId, SqlLineageParser.FieldLineage wildcard) {
        try {
            List<String> sourceColumns = getTableColumns(
                    wildcard.getSourceCatalog(), wildcard.getSourceDatabase(), wildcard.getSourceTable());
            for (String col : sourceColumns) {
                DataLineageField field = new DataLineageField();
                field.setLineageId(lineageId);
                field.setSourceDatabase(wildcard.getSourceDatabase());
                field.setSourceTable(wildcard.getSourceTable());
                field.setSourceField(col);
                field.setTargetDatabase(wildcard.getTargetDatabase());
                field.setTargetTable(wildcard.getTargetTable());
                field.setTargetField(col);
                field.setFieldType("direct");
                fieldMapper.save(field);
            }
            log.info("通配符展开: {}.{} → {} ({}个字段)",
                    wildcard.getSourceDatabase(), wildcard.getSourceTable(),
                    wildcard.getTargetTable(), sourceColumns.size());
        } catch (Exception e) {
            log.warn("通配符展开失败: {}", e.getMessage());
        }
    }

    private List<String> getTableColumns(String catalog, String database, String table) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, String.class, database, table);
    }

    public List<DataLineage> getAllLineages(String groupId) {
        if (groupId != null && !groupId.isBlank()) {
            return lineageMapper.findByGroupIdOrderByCreateTimeDesc(groupId);
        }
        return lineageMapper.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
    }

    public List<DataLineageField> getFieldLineages(String lineageId) {
        return fieldMapper.findByLineageId(lineageId);
    }

    /**
     * 获取血缘图
     * 优先从 OpenMetadata 获取完整血缘图，降级使用本地存储
     */
    public Map<String, Object> getLineageGraph(String groupId) {
        // 先尝试从 OpenMetadata 获取血缘图
        try {
            String searchQuery = groupId != null ? groupId : "*";
            Map<String, Object> omData = omLineageClient.getLineage("table", searchQuery, 3, 3);
            if (omData != null && omData.containsKey("nodes")) {
                log.info("从OpenMetadata获取血缘图成功");
                return omData;
            }
        } catch (Exception e) {
            log.warn("从OpenMetadata获取血缘图失败，降级为本地查询: {}", e.getMessage());
        }

        // 降级：本地构建血缘图
        return buildLocalLineageGraph(groupId);
    }

    private Map<String, Object> buildLocalLineageGraph(String groupId) {
        List<DataLineage> lineages = getAllLineages(groupId);
        Map<String, Object> graph = new HashMap<>();

        Set<Map<String, String>> nodes = new LinkedHashSet<>();
        Set<String> nodeIds = new HashSet<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (DataLineage lineage : lineages) {
            String sourceId = lineage.getSourceTable();
            String targetId = lineage.getTargetTable();
            if (!nodeIds.contains(sourceId)) {
                Map<String, String> node = new HashMap<>();
                node.put("id", sourceId);
                node.put("name", sourceId);
                node.put("type", "source");
                nodes.add(node);
                nodeIds.add(sourceId);
            }
            if (!nodeIds.contains(targetId)) {
                Map<String, String> node = new HashMap<>();
                node.put("id", targetId);
                node.put("name", targetId);
                node.put("type", "target");
                nodes.add(node);
                nodeIds.add(targetId);
            }
            Map<String, Object> edge = new HashMap<>();
            edge.put("source", sourceId);
            edge.put("target", targetId);
            edge.put("lineageId", lineage.getId());
            edge.put("jobType", lineage.getJobType());
            List<DataLineageField> fields = getFieldLineages(lineage.getId());
            edge.put("fieldCount", fields.size());
            edge.put("fields", fields);
            edges.add(edge);
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    @Transactional
    public void deleteLineage(String id) {
        fieldMapper.deleteByLineageId(id);
        lineageMapper.deleteById(id);
    }
}
