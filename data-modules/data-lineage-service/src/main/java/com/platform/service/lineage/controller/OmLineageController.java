package com.platform.service.lineage.controller;

import com.platform.common.om.client.OmLineageClient;
import com.platform.common.om.client.OmSearchClient;
import com.platform.common.om.client.OmTableClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/lineage/om")
@RequiredArgsConstructor
public class OmLineageController {

    private final OmLineageClient lineageClient;
    private final OmTableClient tableClient;
    private final OmSearchClient searchClient;

    @GetMapping("/graph/{entityType}/{id}")
    public Map<String, Object> getLineageGraph(@PathVariable(value = "entityType") String entityType,
                                                @PathVariable(value = "id") String id,
                                                @RequestParam(value = "upstreamDepth", defaultValue = "3") int upstreamDepth,
                                                @RequestParam(value = "downstreamDepth", defaultValue = "3") int downstreamDepth) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = lineageClient.getLineage(entityType, id, upstreamDepth, downstreamDepth);
        result.put("code", data != null ? 0 : 1);
        result.put("data", data);
        return result;
    }

    @PostMapping("/edge")
    public Map<String, Object> addLineageEdge(@RequestBody Map<String, Object> body) {
        String fromId = (String) body.get("fromId");
        String fromType = (String) body.get("fromType");
        String fromFqn = (String) body.get("fromFqn");
        String toId = (String) body.get("toId");
        String toType = (String) body.get("toType");
        String toFqn = (String) body.get("toFqn");
        String sqlQuery = (String) body.get("sqlQuery");

        Map<String, Object> data = lineageClient.addLineage(
                fromId, fromType, fromFqn, toId, toType, toFqn, sqlQuery);

        Map<String, Object> result = new HashMap<>();
        result.put("code", data != null ? 0 : 1);
        result.put("msg", data != null ? "血缘关系添加成功" : "血缘关系添加失败");
        return result;
    }

    @GetMapping("/table/{fqn}")
    public Map<String, Object> getTable(@PathVariable(value = "fqn") String fqn,
                                        @RequestParam(value = "fields", defaultValue = "columns,tags,owner") String fields) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = tableClient.getTableByFqn(fqn, fields);
        result.put("code", data != null ? 0 : 1);
        result.put("data", data);
        return result;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(value = "q") String q,
                                      @RequestParam(value = "index", defaultValue = "all") String index,
                                      @RequestParam(value = "from", defaultValue = "0") int from,
                                      @RequestParam(value = "size", defaultValue = "10") int size) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = searchClient.search(q, index, from, size);
        result.put("code", data != null ? 0 : 1);
        result.put("data", data);
        return result;
    }

    @GetMapping("/tables")
    public Map<String, Object> listTables(@RequestParam(value = "limit", defaultValue = "50") int limit,
                                          @RequestParam(value = "offset", defaultValue = "0") int offset) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = tableClient.listTables(limit, offset);
        result.put("code", data != null ? 0 : 1);
        result.put("data", data);
        return result;
    }
}
