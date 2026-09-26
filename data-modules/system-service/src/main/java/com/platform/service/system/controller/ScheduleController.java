package com.platform.service.system.controller;

import com.platform.common.ds.client.DsExecutorClient;
import com.platform.common.ds.client.DsScheduleClient;
import com.platform.common.ds.client.DsWorkflowClient;
import com.platform.common.ds.model.DsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/schedule/workflow")
@RequiredArgsConstructor
public class ScheduleController {

    private final DsWorkflowClient workflowClient;
    private final DsScheduleClient scheduleClient;
    private final DsExecutorClient executorClient;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                   @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        DsResult<Map<String, Object>> result = workflowClient.listWorkflows(pageNo, pageSize);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("data", result.getData());
        resp.put("msg", result.getMsg());
        return resp;
    }

    @GetMapping("/{code}")
    public Map<String, Object> detail(@PathVariable(value = "code") Long code) {
        DsResult<Map<String, Object>> result = workflowClient.getWorkflow(code);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("data", result.getData());
        return resp;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", "");
        String taskDefinitionJsonObj = (String) body.get("taskDefinitionJsonObj");

        DsResult<Map<String, Object>> result = workflowClient.createWorkflow(name, description, taskDefinitionJsonObj);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "工作流创建成功" : result.getMsg());
        resp.put("data", result.getData());
        return resp;
    }

    @PutMapping("/{code}")
    public Map<String, Object> update(@PathVariable(value = "code") Long code, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String taskDefinitionJsonObj = (String) body.get("taskDefinitionJsonObj");

        DsResult<Map<String, Object>> result = workflowClient.updateWorkflow(code, name, taskDefinitionJsonObj);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "工作流更新成功" : result.getMsg());
        return resp;
    }

    @DeleteMapping("/{code}")
    public Map<String, Object> delete(@PathVariable(value = "code") Long code) {
        DsResult<Void> result = workflowClient.deleteWorkflow(code);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "工作流删除成功" : result.getMsg());
        return resp;
    }

    @PostMapping("/{code}/start")
    public Map<String, Object> start(@PathVariable(value = "code") Long code) {
        String scheduleTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        DsResult<Long> result = executorClient.startWorkflow(code, scheduleTime);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "工作流已启动" : result.getMsg());
        resp.put("data", result.getData());
        return resp;
    }

    @PostMapping("/{code}/schedule")
    public Map<String, Object> createSchedule(@PathVariable(value = "code") Long code, @RequestBody Map<String, String> body) {
        String crontab = body.get("crontab");
        String startTime = body.getOrDefault("startTime", "2026-01-01 00:00:00");
        String endTime = body.getOrDefault("endTime", "2099-12-31 00:00:00");

        DsResult<Long> result = scheduleClient.createSchedule(code, crontab, startTime, endTime);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "调度配置创建成功" : result.getMsg());
        resp.put("data", result.getData());
        return resp;
    }

    @PostMapping("/schedule/{id}/online")
    public Map<String, Object> onlineSchedule(@PathVariable(value = "id") Long id) {
        DsResult<Void> result = scheduleClient.onlineSchedule(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "调度已上线" : result.getMsg());
        return resp;
    }

    @PostMapping("/schedule/{id}/offline")
    public Map<String, Object> offlineSchedule(@PathVariable(value = "id") Long id) {
        DsResult<Void> result = scheduleClient.offlineSchedule(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", result.isSuccess() ? 0 : 1);
        resp.put("msg", result.isSuccess() ? "调度已下线" : result.getMsg());
        return resp;
    }
}
