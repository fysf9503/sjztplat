package com.platform.service.lineage.controller;

import com.platform.service.lineage.entity.DataLineage;
import com.platform.service.lineage.entity.DataLineageField;
import com.platform.service.lineage.service.LineageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(value = "groupId", required = false) String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", lineageService.getAllLineages(groupId));
        return result;
    }

    @GetMapping("/graph")
    public Map<String, Object> graph(@RequestParam(value = "groupId", required = false) String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", lineageService.getLineageGraph(groupId));
        return result;
    }

    @GetMapping("/fields/{lineageId}")
    public Map<String, Object> fields(@PathVariable(value = "lineageId") String lineageId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", lineageService.getFieldLineages(lineageId));
        return result;
    }

    @PostMapping("/parse")
    public Map<String, Object> parseSql(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", lineageService.saveLineageFromSql(
                body.get("sql"),
                body.get("jobId"),
                body.get("jobType"),
                body.get("groupId")));
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable(value = "id") String id) {
        lineageService.deleteLineage(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }
}
