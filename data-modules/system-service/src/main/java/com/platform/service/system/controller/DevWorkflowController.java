package com.platform.service.system.controller;

import com.platform.service.system.entity.DevWorkflow;
import com.platform.service.system.entity.DevWorkflowTask;
import com.platform.service.system.service.DevWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据开发工作流管理
 * 核心流程: 创建工作流 → 添加任务 → 自动检测依赖 → 发布到DS → 上线调度
 */
@Slf4j
@RestController
@RequestMapping("/dev/workflow")
@RequiredArgsConstructor
public class DevWorkflowController {

    private final DevWorkflowService workflowService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(value = "status", required = false) String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        List<DevWorkflow> workflows = workflowService.listWorkflows(status);
        result.put("data", workflows);
        result.put("total", workflows.size());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        DevWorkflow workflow = workflowService.getWorkflow(id);
        result.put("code", workflow != null ? 0 : 1);
        result.put("data", workflow);
        return result;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody DevWorkflow workflow) {
        Map<String, Object> result = new HashMap<>();
        DevWorkflow created = workflowService.createWorkflow(workflow);
        result.put("code", 0);
        result.put("data", created);
        result.put("msg", "工作流创建成功");
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        workflowService.deleteWorkflow(id);
        result.put("code", 0);
        result.put("msg", "工作流删除成功");
        return result;
    }

    /**
     * 添加任务到工作流
     * Body: { "taskId": "xxx", "upstreamTaskIds": ["yyy", "zzz"] }
     * upstreamTaskIds 为空或不传 → 自动推导依赖
     */
    @PostMapping("/{workflowId}/task")
    public Map<String, Object> addTask(@PathVariable(value = "workflowId") String workflowId,
                                      @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String taskId = (String) body.get("taskId");
            @SuppressWarnings("unchecked")
            List<String> upstreamTaskIds = (List<String>) body.get("upstreamTaskIds");

            DevWorkflowTask wft = workflowService.addTaskToWorkflow(workflowId, taskId, upstreamTaskIds);
            result.put("code", 0);
            result.put("data", wft);
            result.put("msg", upstreamTaskIds == null || upstreamTaskIds.isEmpty()
                    ? "任务已添加，依赖关系将自动推导" : "任务已添加（手动指定依赖）");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "添加任务失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{workflowId}/task/{taskId}")
    public Map<String, Object> removeTask(@PathVariable(value = "workflowId") String workflowId,
                                          @PathVariable(value = "taskId") String taskId) {
        Map<String, Object> result = new HashMap<>();
        workflowService.removeTaskFromWorkflow(workflowId, taskId);
        result.put("code", 0);
        result.put("msg", "任务已移除");
        return result;
    }

    /**
     * 获取工作流中的所有任务
     */
    @GetMapping("/{workflowId}/tasks")
    public Map<String, Object> tasks(@PathVariable(value = "workflowId") String workflowId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", workflowService.getWorkflowTasks(workflowId));
        return result;
    }

    /**
     * 获取工作流 DAG 可视化数据（供前端 ECharts 渲染）
     */
    @GetMapping("/{workflowId}/dag")
    public Map<String, Object> dag(@PathVariable(value = "workflowId") String workflowId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", workflowService.getWorkflowDag(workflowId));
        return result;
    }

    /**
     * 获取自动检测的依赖建议（不修改数据库，仅返回建议供用户确认）
     */
    @GetMapping("/{workflowId}/dependency-suggestions")
    public Map<String, Object> getDependencySuggestions(@PathVariable(value = "workflowId") String workflowId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", workflowService.detectDependencySuggestions(workflowId));
        return result;
    }

    /**
     * 确认依赖关系（用户确认后保存）
     * Body: { "taskId1": ["upstreamId1", "upstreamId2"], "taskId2": [] }
     */
    @PostMapping("/{workflowId}/confirm-dependencies")
    public Map<String, Object> confirmDependencies(@PathVariable(value = "workflowId") String workflowId,
                                                     @RequestBody Map<String, List<String>> confirmedDeps) {
        Map<String, Object> result = new HashMap<>();
        try {
            Object dag = workflowService.confirmDependencies(workflowId, confirmedDeps);
            result.put("code", 0);
            result.put("data", dag);
            result.put("msg", "依赖关系已确认");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "确认失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 手动添加前置依赖
     * Body: { "taskId": "xxx", "upstreamTaskId": "yyy" }
     */
    @PostMapping("/{workflowId}/manual-dependency")
    public Map<String, Object> manualAddDependency(@PathVariable(value = "workflowId") String workflowId,
                                                    @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            var wft = workflowService.manualAddDependency(
                    workflowId, body.get("taskId"), body.get("upstreamTaskId"));
            result.put("code", 0);
            result.put("data", wft);
            result.put("msg", "前置依赖已添加");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "添加失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 移除一条前置依赖
     * Body: { "taskId": "xxx", "upstreamTaskId": "yyy" }
     */
    @DeleteMapping("/{workflowId}/dependency")
    public Map<String, Object> removeDependency(@PathVariable(value = "workflowId") String workflowId,
                                                 @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            var wft = workflowService.removeDependency(
                    workflowId, body.get("taskId"), body.get("upstreamTaskId"));
            result.put("code", 0);
            result.put("data", wft);
            result.put("msg", "依赖已移除");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "移除失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 搜索可添加为前置依赖的任务（按任务名模糊搜索）
     */
    @GetMapping("/{workflowId}/search-tasks")
    public Map<String, Object> searchTasks(@PathVariable(value = "workflowId") String workflowId,
                                            @RequestParam(value = "keyword", required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", workflowService.searchTasksForDependency(keyword, workflowId));
        return result;
    }

    /**
     * 发布工作流到 DolphinScheduler（生成完整 DAG 工作流）
     */
    @PostMapping("/{workflowId}/publish")
    public Map<String, Object> publish(@PathVariable(value = "workflowId") String workflowId) {
        Map<String, Object> result = new HashMap<>();
        try {
            DevWorkflow workflow = workflowService.publishWorkflow(workflowId);
            result.put("code", 0);
            result.put("data", workflow);
            result.put("msg", "工作流已发布到DolphinScheduler");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "发布失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 上线工作流（创建调度并上线）
     */
    @PostMapping("/{workflowId}/online")
    public Map<String, Object> online(@PathVariable(value = "workflowId") String workflowId) {
        Map<String, Object> result = new HashMap<>();
        try {
            DevWorkflow workflow = workflowService.onlineWorkflow(workflowId);
            result.put("code", 0);
            result.put("data", workflow);
            result.put("msg", "工作流已上线");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "上线失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 下线工作流
     */
    @PostMapping("/{workflowId}/offline")
    public Map<String, Object> offline(@PathVariable(value = "workflowId") String workflowId) {
        Map<String, Object> result = new HashMap<>();
        try {
            DevWorkflow workflow = workflowService.offlineWorkflow(workflowId);
            result.put("code", 0);
            result.put("data", workflow);
            result.put("msg", "工作流已下线");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "下线失败: " + e.getMessage());
        }
        return result;
    }
}
