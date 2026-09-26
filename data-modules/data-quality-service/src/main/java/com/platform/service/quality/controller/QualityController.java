package com.platform.service.quality.controller;

import com.platform.common.core.R;
import com.platform.service.quality.api.entity.CheckReportEntity;
import com.platform.service.quality.api.entity.CheckRuleEntity;
import com.platform.service.quality.api.entity.ScheduleLogEntity;
import com.platform.service.quality.service.CheckReportService;
import com.platform.service.quality.service.CheckRuleService;
import com.platform.service.quality.service.ScheduleLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "数据质量")
@RestController
@RequestMapping("/quality")
public class QualityController {

    private final CheckRuleService checkRuleService;
    private final CheckReportService checkReportService;
    private final ScheduleLogService scheduleLogService;

    public QualityController(CheckRuleService checkRuleService,
                            CheckReportService checkReportService,
                            ScheduleLogService scheduleLogService) {
        this.checkRuleService = checkRuleService;
        this.checkReportService = checkReportService;
        this.scheduleLogService = scheduleLogService;
    }

    @Operation(summary = "规则分页查询")
    @GetMapping("/rules/page")
    public R<Page<CheckRuleEntity>> rulePage(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return R.ok(checkRuleService.page(PageRequest.of(pageNum - 1, pageSize)));
    }

    @Operation(summary = "新增规则")
    @PostMapping("/rules")
    public R<Void> saveRule(@RequestBody CheckRuleEntity rule) {
        checkRuleService.save(rule);
        return R.ok();
    }

    @Operation(summary = "修改规则")
    @PutMapping("/rules/{id}")
    public R<Void> updateRule(@PathVariable(value = "id") String id, @RequestBody CheckRuleEntity rule) {
        rule.setId(id);
        checkRuleService.save(rule);
        return R.ok();
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/rules/{id}")
    public R<Void> deleteRule(@PathVariable(value = "id") String id) {
        checkRuleService.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "核查报告分页")
    @GetMapping("/reports/page")
    public R<Page<CheckReportEntity>> reportPage(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return R.ok(checkReportService.page(PageRequest.of(pageNum - 1, pageSize)));
    }

    @Operation(summary = "调度日志分页")
    @GetMapping("/logs/page")
    public R<Page<ScheduleLogEntity>> logPage(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return R.ok(scheduleLogService.page(PageRequest.of(pageNum - 1, pageSize)));
    }
}
