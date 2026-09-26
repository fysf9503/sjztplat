package com.platform.service.system.controller;

import com.platform.service.system.entity.JobConfig;
import com.platform.service.system.service.JobConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度配置控制器
 * 前端通过此接口管理所有DolphinScheduler任务的Cron和启停
 */
@RestController
@RequestMapping("/schedule/job")
@RequiredArgsConstructor
public class JobConfigController {

    private final JobConfigService jobConfigService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(value = "module", required = false) String module) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        List<JobConfig> jobs = jobConfigService.listAll(module);
        result.put("data", jobs);
        result.put("total", jobs.size());
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody JobConfig config) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", jobConfigService.addJobConfig(config));
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable(value = "id") String id) {
        jobConfigService.deleteJobConfig(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PutMapping("/{id}/cron")
    public Map<String, Object> updateCron(@PathVariable(value = "id") String id, @RequestBody Map<String, String> body) {
        jobConfigService.updateCron(id, body.get("cronExpr"));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "Cron已更新");
        return result;
    }

    @PutMapping("/{id}/param")
    public Map<String, Object> updateParam(@PathVariable(value = "id") String id, @RequestBody Map<String, String> body) {
        jobConfigService.updateJobParam(id, body.get("jobParam"));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "参数已更新");
        return result;
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> start(@PathVariable(value = "id") String id) {
        jobConfigService.startJob(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "任务已启动");
        return result;
    }

    @PostMapping("/{id}/stop")
    public Map<String, Object> stop(@PathVariable(value = "id") String id) {
        jobConfigService.stopJob(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "任务已停止");
        return result;
    }

    @PostMapping("/{id}/trigger")
    public Map<String, Object> trigger(@PathVariable(value = "id") String id) {
        jobConfigService.triggerJob(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "任务已手动触发");
        return result;
    }
}
