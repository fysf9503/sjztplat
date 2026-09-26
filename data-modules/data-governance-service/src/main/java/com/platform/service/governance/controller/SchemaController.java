package com.platform.service.governance.controller;

import com.platform.service.governance.dto.SchemaDiffResult;
import com.platform.service.governance.entity.MetaTableSchema;
import com.platform.service.governance.entity.SchemaDiffLog;
import com.platform.service.governance.parser.ParamParser;
import com.platform.service.governance.service.AutoDdlService;
import com.platform.service.governance.service.SchemaSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/governance/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaSyncService schemaSyncService;
    private final AutoDdlService autoDdlService;
    private final ParamParser paramParser;

    // ===== 表结构对比 =====

    @GetMapping("/tables")
    public Map<String, Object> listTables(@RequestParam(value = "groupId", required = false) String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", schemaSyncService.listRegisteredTables(groupId));
        return result;
    }

    @PostMapping("/tables")
    public Map<String, Object> registerTable(@RequestBody MetaTableSchema meta) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", schemaSyncService.registerTable(meta));
        return result;
    }

    @DeleteMapping("/tables/{id}")
    public Map<String, Object> unregisterTable(@PathVariable(value = "id") String id) {
        schemaSyncService.unregisterTable(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/compare/{tableSchemaId}")
    public Map<String, Object> compare(@PathVariable(value = "tableSchemaId") String tableSchemaId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", schemaSyncService.compareRegisteredTable(tableSchemaId));
        return result;
    }

    @PostMapping("/compare-all")
    public Map<String, Object> compareAll(@RequestParam(value = "groupId", required = false) String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", schemaSyncService.compareAll(groupId));
        return result;
    }

    @GetMapping("/diffs/{tableSchemaId}")
    public Map<String, Object> getDiffs(@PathVariable(value = "tableSchemaId") String tableSchemaId,
                                        @RequestParam(value = "resolved", required = false) String resolved) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", schemaSyncService.getDiffLogs(tableSchemaId, resolved));
        return result;
    }

    @PostMapping("/diffs/{diffId}/resolve")
    public Map<String, Object> resolveDiff(@PathVariable(value = "diffId") String diffId,
                                             @RequestParam(value = "action") String action,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        schemaSyncService.resolveDiff(diffId, action, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    // ===== 自动建表 =====

    @PostMapping("/auto-ddl/preview")
    public Map<String, Object> previewDdl(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", autoDdlService.generateDdl(
                (String) body.get("catalog"),
                (String) body.get("database"),
                (String) body.get("table"),
                (String) body.get("targetDatabase"),
                (String) body.get("targetTable"),
                (String) body.get("partitionField"),
                (String) body.get("partitionType"),
                (String) body.get("engine"),
                (String) body.get("uniqueKey"),
                body.get("includeComments") == null || (Boolean) body.get("includeComments")));
        return result;
    }

    @PostMapping("/auto-ddl/execute")
    public Map<String, Object> executeDdl(@RequestBody Map<String, String> body) {
        autoDdlService.executeDdl(body.get("ddl"));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "建表成功");
        return result;
    }

    @PostMapping("/auto-ddl/ctas")
    public Map<String, Object> previewCtas(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", autoDdlService.generateCtas(
                (String) body.get("catalog"),
                (String) body.get("database"),
                (String) body.get("table"),
                (String) body.get("targetDatabase"),
                (String) body.get("targetTable"),
                (String) body.get("whereClause")));
        return result;
    }

    @GetMapping("/columns")
    public Map<String, Object> getColumns(@RequestParam(value = "catalog", required = false) String catalog,
                                          @RequestParam(value = "database") String database,
                                          @RequestParam(value = "table") String table) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", schemaSyncService.getTableColumns(catalog, database, table));
        return result;
    }

    @GetMapping("/preview-data")
    public Map<String, Object> previewData(@RequestParam(value = "catalog", required = false) String catalog,
                                            @RequestParam(value = "database") String database,
                                            @RequestParam(value = "table") String table) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", autoDdlService.previewData(catalog, database, table));
        return result;
    }

    // ===== 动态参数 =====

    @GetMapping("/params")
    public Map<String, Object> getParams() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", paramParser.getAvailableParams());
        return result;
    }

    @PostMapping("/params/parse")
    public Map<String, Object> parseParam(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", paramParser.parse(body.get("text")));
        return result;
    }
}
