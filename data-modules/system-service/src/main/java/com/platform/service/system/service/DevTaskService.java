package com.platform.service.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.ds.client.DsWorkflowClient;
import com.platform.common.ds.model.DsResult;
import com.platform.service.system.entity.DevTask;
import com.platform.service.system.mapper.DevTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 开发任务管理服务
 * 创建任务时自动注册到 DolphinScheduler，通过 sourceTables/targetTables 支持自动依赖推导
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevTaskService {

    private final DevTaskMapper taskMapper;
    private final DsWorkflowClient dsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DevTask createTask(DevTask task) {
        task.setId(UUID.randomUUID().toString().replace("-", ""));
        task.setStatus("DRAFT");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        // 注册到 DolphinScheduler
        DsResult<Map<String, Object>> result = dsClient.createTaskDefinition(
                task.getTaskName(),
                task.getTaskType(),
                task.getTaskParams() != null ? task.getTaskParams() : "{}",
                task.getDescription());

        if (result.isSuccess() && result.getData() != null) {
            Object code = result.getData().get("code");
            if (code instanceof Number n) {
                task.setDsTaskCode(n.longValue());
            }
            task.setStatus("PUBLISHED");
            log.info("任务已注册到DS: {} (code={})", task.getTaskName(), task.getDsTaskCode());
        } else {
            log.warn("任务注册到DS失败，仅保存本地: {}", result.getMsg());
        }

        return taskMapper.save(task);
    }

    @Transactional
    public DevTask updateTask(String id, DevTask update) {
        DevTask task = taskMapper.findById(id).orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        task.setTaskName(update.getTaskName());
        task.setTaskType(update.getTaskType());
        task.setTaskParams(update.getTaskParams());
        task.setDescription(update.getDescription());
        task.setSourceTables(update.getSourceTables());
        task.setTargetTables(update.getTargetTables());
        task.setUpdateTime(LocalDateTime.now());

        // 同步到 DS
        if (task.getDsTaskCode() != null && task.getDsTaskCode() > 0) {
            DsResult<Void> result = dsClient.updateTaskDefinition(
                    task.getDsTaskCode(),
                    task.getTaskName(),
                    task.getTaskType(),
                    task.getTaskParams() != null ? task.getTaskParams() : "{}",
                    task.getDescription());
            if (!result.isSuccess()) {
                log.warn("DS 任务定义更新失败: {}", result.getMsg());
            }
        }

        return taskMapper.save(task);
    }

    public DevTask getTask(String id) {
        return taskMapper.findById(id).orElse(null);
    }

    public List<DevTask> listTasks(String taskType, String status) {
        if (taskType != null && !taskType.isBlank()) {
            return taskMapper.findByTaskType(taskType);
        }
        if (status != null && !status.isBlank()) {
            return taskMapper.findByStatus(status);
        }
        return taskMapper.findAll();
    }

    @Transactional
    public void deleteTask(String id) {
        DevTask task = taskMapper.findById(id).orElse(null);
        if (task == null) return;

        if (task.getDsTaskCode() != null && task.getDsTaskCode() > 0) {
            dsClient.deleteTaskDefinition(task.getDsTaskCode());
        }
        taskMapper.deleteById(id);
        log.info("任务已删除: {}", task.getTaskName());
    }

    /**
     * 获取任务的源表列表
     */
    public List<String> getSourceTables(DevTask task) {
        return parseStringList(task.getSourceTables());
    }

    /**
     * 获取任务的目标表列表
     */
    public List<String> getTargetTables(DevTask task) {
        return parseStringList(task.getTargetTables());
    }

    /**
     * 获取 DS 任务定义详情（从 DS 拉取）
     */
    public Map<String, Object> getDsTaskDetail(String id) {
        DevTask task = taskMapper.findById(id).orElse(null);
        if (task == null || task.getDsTaskCode() == null) return null;

        DsResult<Map<String, Object>> result = dsClient.getTaskDefinition(task.getDsTaskCode());
        return result.isSuccess() ? result.getData() : null;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {} → {}", json, e.getMessage());
            return Collections.emptyList();
        }
    }
}
