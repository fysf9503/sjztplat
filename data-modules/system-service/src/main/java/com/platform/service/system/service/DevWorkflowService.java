package com.platform.service.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.ds.client.DsScheduleClient;
import com.platform.common.ds.client.DsWorkflowClient;
import com.platform.common.ds.model.DsResult;
import com.platform.service.system.entity.DevTask;
import com.platform.service.system.entity.DevWorkflow;
import com.platform.service.system.entity.DevWorkflowTask;
import com.platform.service.system.mapper.DevTaskMapper;
import com.platform.service.system.mapper.DevWorkflowMapper;
import com.platform.service.system.mapper.DevWorkflowTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 开发工作流管理服务
 * 核心能力：将多个任务组织成 DAG，自动检测表血缘依赖，一键发布到 DolphinScheduler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevWorkflowService {

    private final DevWorkflowMapper workflowMapper;
    private final DevWorkflowTaskMapper workflowTaskMapper;
    private final DevTaskMapper taskMapper;
    private final DevTaskService devTaskService;
    private final DsWorkflowClient dsClient;
    private final DsScheduleClient scheduleClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== 工作流 CRUD =====

    @Transactional
    public DevWorkflow createWorkflow(DevWorkflow workflow) {
        workflow.setId(UUID.randomUUID().toString().replace("-", ""));
        workflow.setStatus("DRAFT");
        workflow.setCreateTime(LocalDateTime.now());
        workflow.setUpdateTime(LocalDateTime.now());
        return workflowMapper.save(workflow);
    }

    public DevWorkflow getWorkflow(String id) {
        return workflowMapper.findById(id).orElse(null);
    }

    public List<DevWorkflow> listWorkflows(String status) {
        if (status != null && !status.isBlank()) {
            return workflowMapper.findByStatus(status);
        }
        return workflowMapper.findAll();
    }

    @Transactional
    public void deleteWorkflow(String id) {
        DevWorkflow workflow = workflowMapper.findById(id).orElse(null);
        if (workflow == null) return;

        // 下线 DS 调度
        if (workflow.getDsScheduleId() != null && workflow.getDsScheduleId() > 0) {
            scheduleClient.offlineSchedule(workflow.getDsScheduleId());
        }
        // 删除 DS 工作流
        if (workflow.getDsWorkflowCode() != null && workflow.getDsWorkflowCode() > 0) {
            dsClient.deleteWorkflow(workflow.getDsWorkflowCode());
        }
        // 删除本地关联
        workflowTaskMapper.deleteByWorkflowId(id);
        workflowMapper.deleteById(id);
    }

    // ===== 工作流-任务管理 =====

    /**
     * 添加任务到工作流
     * @param workflowId 工作流 ID
     * @param taskId 任务 ID
     * @param upstreamTaskIds 上游任务 ID 列表，传 null 或空表示自动推导
     */
    @Transactional
    public DevWorkflowTask addTaskToWorkflow(String workflowId, String taskId, List<String> upstreamTaskIds) {
        DevWorkflow workflow = workflowMapper.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));
        DevTask task = taskMapper.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

        DevWorkflowTask wft = new DevWorkflowTask();
        wft.setId(UUID.randomUUID().toString().replace("-", ""));
        wft.setWorkflowId(workflowId);
        wft.setTaskId(taskId);
        wft.setDsTaskCode(task.getDsTaskCode());
        wft.setCreateTime(LocalDateTime.now());
        wft.setUpdateTime(LocalDateTime.now());

        if (upstreamTaskIds == null || upstreamTaskIds.isEmpty()) {
            wft.setDependencyType("AUTO");
            wft.setUpstreamTaskIds("[]");
        } else {
            wft.setDependencyType("MANUAL");
            try {
                wft.setUpstreamTaskIds(objectMapper.writeValueAsString(upstreamTaskIds));
            } catch (Exception e) {
                wft.setUpstreamTaskIds("[]");
            }
        }

        return workflowTaskMapper.save(wft);
    }

    /**
     * 从工作流移除任务
     */
    @Transactional
    public void removeTaskFromWorkflow(String workflowId, String taskId) {
        List<DevWorkflowTask> wfts = workflowTaskMapper.findByWorkflowId(workflowId);
        for (DevWorkflowTask wft : wfts) {
            if (taskId.equals(wft.getTaskId())) {
                workflowTaskMapper.deleteById(wft.getId());
            }
        }
    }

    /**
     * 获取工作流中的所有任务（含依赖信息）
     */
    public List<DevWorkflowTask> getWorkflowTasks(String workflowId) {
        return workflowTaskMapper.findByWorkflowId(workflowId);
    }

    // ===== 依赖检测与确认（核心逻辑） =====

    /**
     * 自动检测依赖建议（仅返回建议，不修改数据库）
     * 规则：任务 A 的 sourceTables ∩ 任务 B 的 targetTables ≠ ∅ → 建议 A 依赖 B
     * @return Map: taskId → List<{upstreamTaskId, upstreamTaskName, taskType, matchedTables}>
     */
    public Map<String, List<Map<String, Object>>> detectDependencySuggestions(String workflowId) {
        List<DevWorkflowTask> wfts = workflowTaskMapper.findByWorkflowId(workflowId);
        Map<String, DevTask> taskMap = loadTaskMap(wfts);

        Map<String, List<Map<String, Object>>> suggestions = new LinkedHashMap<>();

        for (DevWorkflowTask wft : wfts) {
            DevTask currentTask = taskMap.get(wft.getTaskId());
            if (currentTask == null) continue;

            List<String> mySourceTables = devTaskService.getSourceTables(currentTask);
            List<Map<String, Object>> upstreamList = new ArrayList<>();

            if (!mySourceTables.isEmpty()) {
                for (DevWorkflowTask other : wfts) {
                    if (other.getTaskId().equals(wft.getTaskId())) continue;
                    DevTask otherTask = taskMap.get(other.getTaskId());
                    if (otherTask == null) continue;

                    List<String> otherTargetTables = devTaskService.getTargetTables(otherTask);
                    List<String> matched = new ArrayList<>();
                    for (String target : otherTargetTables) {
                        if (mySourceTables.contains(target)) {
                            matched.add(target);
                        }
                    }
                    if (!matched.isEmpty()) {
                        Map<String, Object> up = new HashMap<>();
                        up.put("upstreamTaskId", other.getTaskId());
                        up.put("upstreamTaskName", otherTask.getTaskName());
                        up.put("taskType", otherTask.getTaskType());
                        up.put("matchedTables", matched);
                        upstreamList.add(up);
                    }
                }
            }

            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", wft.getTaskId());
            taskInfo.put("taskName", currentTask.getTaskName());
            taskInfo.put("currentDependencyType", wft.getDependencyType());
            taskInfo.put("currentUpstreamIds", parseStringList(wft.getUpstreamTaskIds()));
            taskInfo.put("suggestions", upstreamList);
            suggestions.put(wft.getTaskId(), List.of(taskInfo));
        }
        return suggestions;
    }

    /**
     * 确认依赖关系（用户确认后保存到数据库）
     * @param confirmedDeps Map: taskId → List<upstreamTaskId>
     */
    @Transactional
    public Map<String, Object> confirmDependencies(String workflowId, Map<String, List<String>> confirmedDeps) {
        List<DevWorkflowTask> wfts = workflowTaskMapper.findByWorkflowId(workflowId);

        for (DevWorkflowTask wft : wfts) {
            List<String> upstreamIds = confirmedDeps.getOrDefault(wft.getTaskId(), Collections.emptyList());
            try {
                wft.setUpstreamTaskIds(objectMapper.writeValueAsString(upstreamIds));
            } catch (Exception e) {
                wft.setUpstreamTaskIds("[]");
            }
            wft.setDependencyType("CONFIRMED");
            wft.setUpdateTime(LocalDateTime.now());
            workflowTaskMapper.save(wft);
        }

        log.info("工作流 {} 依赖关系已确认", workflowId);
        return getWorkflowDag(workflowId);
    }

    /**
     * 手动添加前置依赖
     */
    @Transactional
    public DevWorkflowTask manualAddDependency(String workflowId, String taskId, String upstreamTaskId) {
        DevWorkflowTask wft = findWorkflowTask(workflowId, taskId);
        List<String> upstreamIds = new ArrayList<>(parseStringList(wft.getUpstreamTaskIds()));
        if (!upstreamIds.contains(upstreamTaskId)) {
            upstreamIds.add(upstreamTaskId);
        }
        try {
            wft.setUpstreamTaskIds(objectMapper.writeValueAsString(upstreamIds));
        } catch (Exception e) {
            wft.setUpstreamTaskIds("[]");
        }
        wft.setDependencyType("MANUAL");
        wft.setUpdateTime(LocalDateTime.now());
        return workflowTaskMapper.save(wft);
    }

    /**
     * 移除一条前置依赖
     */
    @Transactional
    public DevWorkflowTask removeDependency(String workflowId, String taskId, String upstreamTaskId) {
        DevWorkflowTask wft = findWorkflowTask(workflowId, taskId);
        List<String> upstreamIds = new ArrayList<>(parseStringList(wft.getUpstreamTaskIds()));
        upstreamIds.remove(upstreamTaskId);
        try {
            wft.setUpstreamTaskIds(objectMapper.writeValueAsString(upstreamIds));
        } catch (Exception e) {
            wft.setUpstreamTaskIds("[]");
        }
        wft.setUpdateTime(LocalDateTime.now());
        return workflowTaskMapper.save(wft);
    }

    /**
     * 搜索可依赖的任务（按任务名模糊搜索，排除已在工作流中的任务）
     */
    public List<Map<String, Object>> searchTasksForDependency(String keyword, String workflowId) {
        List<DevTask> tasks;
        if (keyword == null || keyword.isBlank()) {
            tasks = taskMapper.findAll();
        } else {
            tasks = taskMapper.findByTaskNameContaining(keyword);
        }

        // 排除已在工作流中的任务
        Set<String> existingTaskIds = new HashSet<>();
        for (DevWorkflowTask wft : workflowTaskMapper.findByWorkflowId(workflowId)) {
            existingTaskIds.add(wft.getTaskId());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (DevTask t : tasks) {
            if (existingTaskIds.contains(t.getId())) continue;
            Map<String, Object> info = new HashMap<>();
            info.put("id", t.getId());
            info.put("taskName", t.getTaskName());
            info.put("taskType", t.getTaskType());
            info.put("description", t.getDescription());
            info.put("status", t.getStatus());
            info.put("sourceTables", devTaskService.getSourceTables(t));
            info.put("targetTables", devTaskService.getTargetTables(t));
            results.add(info);
        }
        return results;
    }

    // ===== 私有辅助方法 =====

    private Map<String, DevTask> loadTaskMap(List<DevWorkflowTask> wfts) {
        Map<String, DevTask> taskMap = new HashMap<>();
        for (DevWorkflowTask wft : wfts) {
            DevTask task = taskMapper.findById(wft.getTaskId()).orElse(null);
            if (task != null) {
                taskMap.put(wft.getTaskId(), task);
            }
        }
        return taskMap;
    }

    private DevWorkflowTask findWorkflowTask(String workflowId, String taskId) {
        return workflowTaskMapper.findByWorkflowId(workflowId).stream()
                .filter(w -> taskId.equals(w.getTaskId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "工作流任务不存在: workflowId=" + workflowId + ", taskId=" + taskId));
    }

    /**
     * 获取工作流的 DAG 可视化数据（供前端 ECharts/DAG 图渲染）
     */
    public Map<String, Object> getWorkflowDag(String workflowId) {
        List<DevWorkflowTask> wfts = workflowTaskMapper.findByWorkflowId(workflowId);
        Map<String, DevTask> taskMap = loadTaskMap(wfts);

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (DevWorkflowTask wft : wfts) {
            DevTask task = taskMap.get(wft.getTaskId());
            if (task == null) continue;

            // 节点
            Map<String, Object> node = new HashMap<>();
            node.put("id", wft.getTaskId());
            node.put("name", task.getTaskName());
            node.put("taskType", task.getTaskType());
            node.put("dsTaskCode", task.getDsTaskCode());
            node.put("sourceTables", devTaskService.getSourceTables(task));
            node.put("targetTables", devTaskService.getTargetTables(task));
            nodes.add(node);

            // 边（上游 → 当前）
            List<String> upstreamIds = parseStringList(wft.getUpstreamTaskIds());
            for (String upId : upstreamIds) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("source", upId);
                edge.put("target", wft.getTaskId());
                edge.put("type", wft.getDependencyType());
                edges.add(edge);
            }
        }

        Map<String, Object> dag = new HashMap<>();
        dag.put("nodes", nodes);
        dag.put("edges", edges);
        return dag;
    }

    // ===== 发布到 DolphinScheduler =====

    /**
     * 发布工作流到 DS（创建带 DAG 依赖的工作流定义）
     * 发布前检查：所有任务的依赖关系必须已确认（CONFIRMED）
     */
    @Transactional
    public DevWorkflow publishWorkflow(String workflowId) {
        DevWorkflow workflow = workflowMapper.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

        List<DevWorkflowTask> wfts = workflowTaskMapper.findByWorkflowId(workflowId);
        if (wfts.isEmpty()) {
            throw new RuntimeException("工作流中没有任务，无法发布");
        }

        // 检查是否有未确认依赖的任务
        List<String> unconfirmedTasks = new ArrayList<>();
        for (DevWorkflowTask wft : wfts) {
            if (!"CONFIRMED".equals(wft.getDependencyType()) && !"MANUAL".equals(wft.getDependencyType())) {
                unconfirmedTasks.add(wft.getTaskId());
            }
        }
        if (!unconfirmedTasks.isEmpty()) {
            throw new RuntimeException("存在未确认依赖的任务（" + unconfirmedTasks.size() + "个），请先确认依赖关系后再发布");
        }

        // 构建 DS taskDefinitionJson 和 taskRelationJson
        String taskDefinitionJson = buildTaskDefinitionJson(wfts);
        String taskRelationJson = buildTaskRelationJson(wfts);

        log.info("发布工作流到DS: {} ({}个任务)", workflow.getWorkflowName(), wfts.size());

        DsResult<Map<String, Object>> result;
        if (workflow.getDsWorkflowCode() != null && workflow.getDsWorkflowCode() > 0) {
            // 更新已有工作流
            result = dsClient.updateWorkflowWithDag(
                    workflow.getDsWorkflowCode(),
                    workflow.getWorkflowName(),
                    workflow.getDescription(),
                    taskDefinitionJson,
                    taskRelationJson);
        } else {
            // 创建新工作流
            result = dsClient.createWorkflowWithDag(
                    workflow.getWorkflowName(),
                    workflow.getDescription(),
                    taskDefinitionJson,
                    taskRelationJson);
        }

        if (result.isSuccess()) {
            if (workflow.getDsWorkflowCode() == null && result.getData() != null) {
                Object code = result.getData().get("code");
                if (code instanceof Number n) {
                    workflow.setDsWorkflowCode(n.longValue());
                }
            }
            workflow.setStatus("PUBLISHED");
            workflow.setUpdateTime(LocalDateTime.now());
            workflowMapper.save(workflow);
            log.info("工作流发布成功: {} (DS code={})", workflow.getWorkflowName(), workflow.getDsWorkflowCode());
        } else {
            throw new RuntimeException("DS 发布失败: " + result.getMsg());
        }

        return workflow;
    }

    /**
     * 上线工作流（创建调度并上线）
     */
    @Transactional
    public DevWorkflow onlineWorkflow(String workflowId) {
        DevWorkflow workflow = workflowMapper.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

        if (workflow.getDsWorkflowCode() == null) {
            throw new RuntimeException("工作流尚未发布到DS，请先 publish");
        }

        // 如果有 Cron 表达式，创建调度
        if (workflow.getCronExpr() != null && !workflow.getCronExpr().isBlank()) {
            // 先下线旧调度
            if (workflow.getDsScheduleId() != null && workflow.getDsScheduleId() > 0) {
                scheduleClient.offlineSchedule(workflow.getDsScheduleId());
            }

            DsResult<Long> schedResult = scheduleClient.createSchedule(
                    workflow.getDsWorkflowCode(),
                    workflow.getCronExpr(),
                    LocalDateTime.now().format(DT_FMT),
                    "2099-12-31 23:59:59");

            if (schedResult.isSuccess() && schedResult.getData() != null) {
                workflow.setDsScheduleId(schedResult.getData());
                scheduleClient.onlineSchedule(schedResult.getData());
            }
        }

        workflow.setStatus("ONLINE");
        workflow.setUpdateTime(LocalDateTime.now());
        workflowMapper.save(workflow);
        log.info("工作流已上线: {}", workflow.getWorkflowName());
        return workflow;
    }

    /**
     * 下线工作流
     */
    @Transactional
    public DevWorkflow offlineWorkflow(String workflowId) {
        DevWorkflow workflow = workflowMapper.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

        if (workflow.getDsScheduleId() != null && workflow.getDsScheduleId() > 0) {
            scheduleClient.offlineSchedule(workflow.getDsScheduleId());
        }

        workflow.setStatus("OFFLINE");
        workflow.setUpdateTime(LocalDateTime.now());
        workflowMapper.save(workflow);
        log.info("工作流已下线: {}", workflow.getWorkflowName());
        return workflow;
    }

    // ===== DS JSON 构建 =====

    /**
     * 构建 DS taskDefinitionJsonObj
     * 格式: [{code, name, taskType, taskParams, ...}, ...]
     */
    private String buildTaskDefinitionJson(List<DevWorkflowTask> wfts) {
        List<Map<String, Object>> definitions = new ArrayList<>();

        for (DevWorkflowTask wft : wfts) {
            DevTask task = taskMapper.findById(wft.getTaskId()).orElse(null);
            if (task == null || task.getDsTaskCode() == null) continue;

            Map<String, Object> def = new HashMap<>();
            def.put("code", task.getDsTaskCode());
            def.put("name", task.getTaskName());
            def.put("taskType", task.getTaskType());
            def.put("description", task.getDescription() != null ? task.getDescription() : "");
            def.put("flag", "YES");
            def.put("taskPriority", "MEDIUM");
            def.put("workerGroup", "default");
            def.put("failRetryTimes", 0);
            def.put("failRetryInterval", 1);
            def.put("timeoutFlag", "CLOSE");
            def.put("timeout", 0);
            def.put("delayTime", 0);

            // taskParams 解析为 Map
            try {
                Map<String, Object> params = objectMapper.readValue(
                        task.getTaskParams() != null ? task.getTaskParams() : "{}",
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
                def.put("taskParams", params);
            } catch (Exception e) {
                def.put("taskParams", Collections.emptyMap());
            }

            definitions.add(def);
        }

        try {
            return objectMapper.writeValueAsString(definitions);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 构建 DS taskRelationJson
     * 格式: [{preTaskCode, postTaskCode, conditionType, conditionParams}, ...]
     * preTaskCode=0 表示起始节点
     */
    private String buildTaskRelationJson(List<DevWorkflowTask> wfts) {
        List<Map<String, Object>> relations = new ArrayList<>();

        // 构建 taskId → dsTaskCode 映射
        Map<String, Long> taskCodeMap = new HashMap<>();
        for (DevWorkflowTask wft : wfts) {
            if (wft.getDsTaskCode() != null) {
                taskCodeMap.put(wft.getTaskId(), wft.getDsTaskCode());
            }
        }

        // 为每个任务生成边
        for (DevWorkflowTask wft : wfts) {
            Long postCode = taskCodeMap.get(wft.getTaskId());
            if (postCode == null) continue;

            List<String> upstreamIds = parseStringList(wft.getUpstreamTaskIds());

            if (upstreamIds.isEmpty()) {
                // 无上游 → 连接到起始节点 (preTaskCode=0)
                Map<String, Object> rel = new HashMap<>();
                rel.put("preTaskCode", 0);
                rel.put("postTaskCode", postCode);
                rel.put("conditionType", "NONE");
                rel.put("conditionParams", Collections.emptyMap());
                relations.add(rel);
            } else {
                // 有上游 → 为每条上游依赖生成一条边
                for (String upId : upstreamIds) {
                    Long preCode = taskCodeMap.get(upId);
                    if (preCode == null) continue;

                    Map<String, Object> rel = new HashMap<>();
                    rel.put("preTaskCode", preCode);
                    rel.put("postTaskCode", postCode);
                    rel.put("conditionType", "NONE");
                    rel.put("conditionParams", Collections.emptyMap());
                    relations.add(rel);
                }
            }
        }

        try {
            return objectMapper.writeValueAsString(relations);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
