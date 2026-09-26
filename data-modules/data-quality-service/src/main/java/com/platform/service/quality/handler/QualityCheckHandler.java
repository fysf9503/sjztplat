package com.platform.service.quality.handler;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.platform.service.quality.api.entity.CheckReportEntity;
import com.platform.service.quality.api.entity.CheckRuleEntity;
import com.platform.service.quality.api.entity.ScheduleLogEntity;
import com.platform.service.quality.service.CheckReportService;
import com.platform.service.quality.service.CheckRuleService;
import com.platform.service.quality.service.ScheduleLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 数据质量核查 Handler
 *
 * 【改造说明】本类替代了原 datax-cloud-pro 项目中以下 12 个类的全部功能：
 * - Quartz 系统：ScheduleJob, ScheduleRunnable, ScheduleUtil, SchedulerConfig, DataSourceConfig, StartedUpRunner
 * - TaskScheduler 系统：CronTaskRegistrar, ScheduledTask, SchedulingRunnable, SchedulingConfig, StartedUpRunner
 *
 * 原理对比：
 *   原方案：数据库存 cron → 启动时 StartedUpRunner 初始化 → Quartz/TaskScheduler 按 cron 触发 → 反射调用 Bean 方法
 *   DolphinScheduler：DS 控制台配置 cron → 调度中心按 cron 触发 → HTTP 调用 REST 接口
 *
 * 被替代的功能映射：
 *   ScheduleUtil.createScheduleJob() → DolphinScheduler 控制台新增任务
 *   ScheduleUtil.pauseJob()          → DolphinScheduler 控制台停止任务
 *   ScheduleUtil.resumeJob()         → DolphinScheduler 控制台启动任务
 *   ScheduleUtil.runJob()            → DolphinScheduler 控制台执行一次
 *   ScheduleUtil.deleteJob()         → DolphinScheduler 控制台删除任务
 *   CronTaskRegistrar.addCronTask()  → DolphinScheduler 控制台新增任务
 *   CronTaskRegistrar.removeCronTask()→ DolphinScheduler 控制台停止任务
 *   StartedUpRunner                  → DolphinScheduler 工作流自动调度
 *   ScheduleRunnable 反射调用         → 直接调用 REST 接口
 *   Quartz 集群 (isClustered)        → DolphinScheduler 天然分布式调度
 *   QrtzJobLogService 日志           → DolphinScheduler 自带执行日志
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class QualityCheckHandler {

    private final CheckRuleService checkRuleService;
    private final CheckReportService checkReportService;
    private final ScheduleLogService scheduleLogService;

    /**
     * 全量数据质量核查（替代原 QualityTask.task(Map) + SchedulingRunnable.run()）
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/qualityCheck
     *   - Cron: 0 0 2 * * ?  （每天凌晨2点）
     *   - 参数: 无（全量核查所有启用的规则）
     */
    @PostMapping("/qualityCheck")
    public void qualityCheckHandler(@RequestParam(value = "param", required = false) String param) {
        log.info("数据质量核查任务开始 - 参数: {}", param);
        long startTime = System.currentTimeMillis();
        String batch = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN);

        List<CheckRuleEntity> rules = checkRuleService.listEnabledRules();
        log.info("启用核查规则数量: {}", rules.size());

        if (rules.isEmpty()) {
            log.info("无启用的核查规则");
            return;
        }

        // 使用虚拟线程（JDK 21）替代原 ThreadPoolExecutor，并行执行核查规则
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<CheckReportEntity>> futures = new ArrayList<>();
            for (CheckRuleEntity rule : rules) {
                futures.add(executor.submit(() -> executeCheckRule(rule, batch)));
            }

            for (Future<CheckReportEntity> future : futures) {
                try {
                    CheckReportEntity report = future.get();
                    processReport(report, batch);
                } catch (InterruptedException | ExecutionException e) {
                    log.error("核查规则执行异常", e);
                    log.error("核查规则执行异常: {}", e.getMessage());
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("数据质量核查任务结束 - 耗时: {}ms", elapsed);
        log.info("数据质量核查完成，共核查 {} 条规则，耗时 {}ms", rules.size(), elapsed);
    }

    /**
     * 单规则核查（可按规则ID指定核查单条规则）
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/singleRuleCheck
     *   - 参数: 规则ID
     */
    @PostMapping("/singleRuleCheck")
    public void singleRuleCheckHandler(@RequestParam(value = "ruleId", required = false) String ruleId) {
        if (StrUtil.isBlank(ruleId)) {
            log.error("参数缺失：请传入规则ID");
            return;
        }

        CheckRuleEntity rule = checkRuleService.getById(ruleId);
        if (rule == null) {
            log.error("规则不存在: {}", ruleId);
            return;
        }

        String batch = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN);
        log.info("单规则核查 - 规则: {}", rule.getRuleName());

        CheckReportEntity report = executeCheckRule(rule, batch);
        processReport(report, batch);
        log.info("规则 [{}] 核查完成", rule.getRuleName());
    }

    /**
     * 执行单条核查规则（从原 QualityTask.TaskHander.call() 迁移）
     * 使用 Doris JDBC 执行核查 SQL
     */
    private CheckReportEntity executeCheckRule(CheckRuleEntity rule, String batch) {
        log.info("执行核查规则 - ID: {}, 名称: {}", rule.getId(), rule.getRuleName());

        CheckReportEntity report = new CheckReportEntity();
        report.setCheckRuleId(rule.getId());
        report.setCheckDate(LocalDateTime.now());
        report.setCheckBatch(batch);

        // 通过 Doris JDBC 执行核查 SQL
        String dorisUrl = "jdbc:mysql://localhost:9030/data_platform";
        try (Connection conn = DriverManager.getConnection(dorisUrl, "root", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(rule.getRuleSql())) {

            while (rs.next()) {
                report.setCheckErrorCount(rs.getInt(1));
                report.setCheckTotalCount(rs.getInt(2));
            }
            log.info("规则 [{}] 核查完成 - 异常数: {}, 总数: {}",
                    rule.getRuleName(), report.getCheckErrorCount(), report.getCheckTotalCount());
        } catch (SQLException e) {
            report.setCheckResult(e.getMessage());
            log.error("规则 [{}] 核查失败: {}", rule.getRuleName(), e.getMessage());
        }

        return report;
    }

    /**
     * 处理核查结果（从原 QualityTask.task() 结果处理逻辑迁移）
     */
    private void processReport(CheckReportEntity report, String batch) {
        String status = StrUtil.isBlank(report.getCheckResult()) ? "1" : "0";

        if (StrUtil.isBlank(report.getCheckResult())) {
            checkReportService.save(report);

            // 更新最近核查批次号
            CheckRuleEntity rule = checkRuleService.getById(report.getCheckRuleId());
            if (rule != null) {
                rule.setLastCheckBatch(batch);
                checkRuleService.save(rule);
            }
        }

        // 记录调度日志
        ScheduleLogEntity logEntity = new ScheduleLogEntity();
        logEntity.setExecuteBatch(batch);
        logEntity.setExecuteDate(report.getCheckDate());
        logEntity.setExecuteRuleId(report.getCheckRuleId());
        logEntity.setExecuteResult(report.getCheckResult());
        logEntity.setStatus(status);
        scheduleLogService.save(logEntity);
    }
}
