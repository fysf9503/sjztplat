package com.platform.service.governance.handler;

import com.platform.service.governance.service.SchemaSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 定时表结构对比 Handler
 * 在 DolphinScheduler 中配置定时任务，参数传 groupId 可只对比指定用户组的表
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class SchemaSyncHandler {

    private final SchemaSyncService schemaSyncService;

    @PostMapping("/schemaSync")
    public void schemaSyncHandler(@RequestParam(value = "param", required = false) String param) {
        log.info("定时表结构对比任务开始 - 参数: {}", param);

        var results = schemaSyncService.compareAll(param);
        int totalDiffs = results.stream().mapToInt(r -> r.getDiffs().size()).sum();

        log.info("对比完成: {}张表, {}处差异", results.size(), totalDiffs);
        log.info("定时表结构对比完成: {}张表, {}处差异", results.size(), totalDiffs);
    }
}
