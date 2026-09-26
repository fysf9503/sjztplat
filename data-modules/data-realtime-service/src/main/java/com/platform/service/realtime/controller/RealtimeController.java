package com.platform.service.realtime.controller;

import com.platform.common.core.R;
import com.platform.service.realtime.service.DorisStreamLoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "实时任务")
@RestController
@RequestMapping("/realtime")
public class RealtimeController {

    private final DorisStreamLoadService streamLoadService;

    public RealtimeController(DorisStreamLoadService streamLoadService) {
        this.streamLoadService = streamLoadService;
    }

    @Operation(summary = "手动触发 Stream Load 写入")
    @PostMapping("/stream-load/{table}")
    public R<String> streamLoad(@PathVariable(value = "table") String table, @RequestBody String jsonData) {
        String result = streamLoadService.streamLoadJson(table, jsonData);
        return R.ok(result);
    }

    @Operation(summary = "手动触发 CSV 写入")
    @PostMapping("/stream-load/{table}/csv")
    public R<String> streamLoadCsv(@PathVariable(value = "table") String table,
                                  @RequestParam(value = "columns") String columns,
                                  @RequestBody String csvData) {
        String result = streamLoadService.streamLoadCsv(table, csvData, columns);
        return R.ok(result);
    }

    @Operation(summary = "查询 Doris 表数据")
    @GetMapping("/query")
    public R<Map<String, Object>> query(@RequestParam(value = "sql") String sql) {
        // 简化：实际应通过 JdbcTemplate 执行查询
        return R.ok(Map.of("sql", sql, "message", "请通过 /realtime/stream-load 手动写入数据"));
    }
}
