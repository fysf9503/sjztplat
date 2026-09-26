package com.platform.service.system.service;

import com.platform.common.ds.client.DsExecutorClient;
import com.platform.common.ds.client.DsScheduleClient;
import com.platform.common.ds.client.DsWorkflowClient;
import com.platform.common.ds.model.DsResult;
import com.platform.service.system.entity.JobConfig;
import com.platform.service.system.mapper.JobConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 调度配置服务
 * 通过 DolphinScheduler REST API 动态创建/更新/启停工作流
 * Cron 表达式由前端配置，不再后端写死
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobConfigService {

    private final JobConfigMapper jobConfigMapper;
    private final DsWorkflowClient workflowClient;
    private final DsScheduleClient scheduleClient;
    private final DsExecutorClient executorClient;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<JobConfig> listAll(String module) {
        Sort sort = Sort.by(Sort.Direction.ASC, "module");
        if (module != null && !module.isBlank()) {
            return jobConfigMapper.findByModule(module, sort);
        }
        return jobConfigMapper.findAll(sort);
    }

    /**
     * 更新 Cron 表达式（前端配置）
     * 同步到 DolphinScheduler 调度配置
     */
    @Transactional
    public void updateCron(String jobConfigId, String cronExpr) {
        JobConfig config = jobConfigMapper.findById(jobConfigId).orElse(null);
        if (config == null) {
            throw new RuntimeException("任务配置不存在: " + jobConfigId);
        }

        config.setCronExpr(cronExpr);
        config.setUpdateTime(LocalDateTime.now());
        jobConfigMapper.save(config);

        if (config.getDsWorkflowCode() != null && config.getDsWorkflowCode() > 0) {
            // 先下线旧调度
            if (config.getDsScheduleId() != null && config.getDsScheduleId() > 0) {
                scheduleClient.offlineSchedule(config.getDsScheduleId());
            }
            // 创建新调度并上线
            DsResult<Long> schedResult = scheduleClient.createSchedule(
                    config.getDsWorkflowCode(), cronExpr,
                    LocalDateTime.now().format(DT_FMT), "2099-12-31 23:59:59");
            if (schedResult.isSuccess() && schedResult.getData() != null) {
                config.setDsScheduleId(schedResult.getData());
                scheduleClient.onlineSchedule(schedResult.getData());
                jobConfigMapper.save(config);
            }
            log.info("Cron已更新: {} → {} (DS Workflow Code: {})", config.getJobName(), cronExpr, config.getDsWorkflowCode());
        }
    }

    /**
     * 启动任务：在 DS 中创建工作流 + 调度并上线
     */
    @Transactional
    public void startJob(String jobConfigId) {
        JobConfig config = jobConfigMapper.findById(jobConfigId).orElse(null);
        if (config == null) return;

        // 如果尚未在 DS 中创建工作流，则创建
        if (config.getDsWorkflowCode() == null || config.getDsWorkflowCode() <= 0) {
            DsResult<Map<String, Object>> result = workflowClient.createWorkflow(
                    config.getJobName(),
                    config.getDescription(),
                    buildTaskDefinitionJson(config));
            if (result.isSuccess() && result.getData() != null) {
                Object code = result.getData().get("code");
                if (code instanceof Number n) {
                    config.setDsWorkflowCode(n.longValue());
                }
            } else {
                log.error("DolphinScheduler 创建工作流失败: {}", result.getMsg());
                throw new RuntimeException("创建工作流失败: " + result.getMsg());
            }
        }

        // 创建调度并上线
        if (config.getCronExpr() != null && !config.getCronExpr().isBlank()) {
            DsResult<Long> schedResult = scheduleClient.createSchedule(
                    config.getDsWorkflowCode(), config.getCronExpr(),
                    LocalDateTime.now().format(DT_FMT), "2099-12-31 23:59:59");
            if (schedResult.isSuccess() && schedResult.getData() != null) {
                config.setDsScheduleId(schedResult.getData());
                scheduleClient.onlineSchedule(schedResult.getData());
            }
        }

        config.setStatus("1");
        config.setUpdateTime(LocalDateTime.now());
        jobConfigMapper.save(config);
        log.info("任务已启动: {} (DS Workflow Code: {})", config.getJobName(), config.getDsWorkflowCode());
    }

    /**
     * 停止任务：下线 DS 调度
     */
    @Transactional
    public void stopJob(String jobConfigId) {
        JobConfig config = jobConfigMapper.findById(jobConfigId).orElse(null);
        if (config == null || config.getDsScheduleId() == null) return;

        scheduleClient.offlineSchedule(config.getDsScheduleId());

        config.setStatus("0");
        config.setUpdateTime(LocalDateTime.now());
        jobConfigMapper.save(config);
        log.info("任务已停止: {} (DS Schedule ID: {})", config.getJobName(), config.getDsScheduleId());
    }

    /**
     * 立即执行一次：启动 DS 工作流实例
     */
    public void triggerJob(String jobConfigId) {
        JobConfig config = jobConfigMapper.findById(jobConfigId).orElse(null);
        if (config == null || config.getDsWorkflowCode() == null) return;

        String scheduleTime = LocalDateTime.now().format(DT_FMT);
        DsResult<Long> result = executorClient.startWorkflow(config.getDsWorkflowCode(), scheduleTime);
        if (result.isSuccess()) {
            log.info("任务已手动触发: {} (执行实例ID: {})", config.getJobName(), result.getData());
        } else {
            log.error("手动触发失败: {}", result.getMsg());
        }
    }

    /**
     * 更新任务参数
     */
    @Transactional
    public void updateJobParam(String jobConfigId, String jobParam) {
        JobConfig config = jobConfigMapper.findById(jobConfigId).orElse(null);
        if (config == null) return;

        config.setJobParam(jobParam);
        config.setUpdateTime(LocalDateTime.now());
        jobConfigMapper.save(config);

        if (config.getDsWorkflowCode() != null && config.getDsWorkflowCode() > 0) {
            workflowClient.updateWorkflow(config.getDsWorkflowCode(),
                    config.getJobName(), buildTaskDefinitionJson(config));
        }
    }

    /**
     * 新增任务配置
     */
    @Transactional
    public JobConfig addJobConfig(JobConfig config) {
        config.setStatus("0");
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return jobConfigMapper.save(config);
    }

    /**
     * 删除任务配置：同时删除 DS 中的工作流
     */
    @Transactional
    public void deleteJobConfig(String jobConfigId) {
        JobConfig config = jobConfigMapper.findById(jobConfigId).orElse(null);
        if (config != null && config.getDsWorkflowCode() != null && config.getDsWorkflowCode() > 0) {
            // 先下线调度
            if (config.getDsScheduleId() != null && config.getDsScheduleId() > 0) {
                scheduleClient.offlineSchedule(config.getDsScheduleId());
            }
            workflowClient.deleteWorkflow(config.getDsWorkflowCode());
        }
        jobConfigMapper.deleteById(jobConfigId);
    }

    /**
     * 构建 DolphinScheduler 任务定义 JSON
     * 将本地 JobConfig 转为 DS Shell 任务
     */
    private String buildTaskDefinitionJson(JobConfig config) {
        String shellContent = config.getHandler() != null ? config.getHandler() : "echo 'no handler'";
        if (config.getJobParam() != null && !config.getJobParam().isBlank()) {
            shellContent += " " + config.getJobParam();
        }

        return """
            [{
                "name": "%s",
                "description": "%s",
                "taskType": "SHELL",
                "taskParams": {
                    "resourceList": [],
                    "localParams": [],
                    "shellContent": "%s",
                    "runFlag": "NORMAL"
                },
                "flag": "YES",
                "taskPriority": "MEDIUM",
                "workerGroup": "default",
                "failRetryTimes": "0",
                "failRetryInterval": "1",
                "timeoutFlag": "CLOSE",
                "timeoutNotifyStrategy": "",
                "timeout": "0"
            }]
            """.formatted(
                config.getJobName(),
                config.getDescription() != null ? config.getDescription() : "",
                shellContent);
    }
}
