package com.platform.service.system.controller;

import com.platform.service.system.entity.DevTask;
import com.platform.service.system.service.DevTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据开发任务管理
 * 前端通过此接口创建 Flink/SQL/Shell 等任务，系统自动注册到 DolphinScheduler
 */
@RestController
@RequestMapping("/dev/task")
@RequiredArgsConstructor
public class DevTaskController {

    private final DevTaskService devTaskService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(value = "taskType", required = false) String taskType,
                                    @RequestParam(value = "status", required = false) String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        List<DevTask> tasks = devTaskService.listTasks(taskType, status);
        result.put("data", tasks);
        result.put("total", tasks.size());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        DevTask task = devTaskService.getTask(id);
        result.put("code", task != null ? 0 : 1);
        result.put("data", task);
        return result;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody DevTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            DevTask created = devTaskService.createTask(task);
            result.put("code", 0);
            result.put("data", created);
            result.put("msg", "任务创建成功，已注册到DolphinScheduler");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "任务创建失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable(value = "id") String id, @RequestBody DevTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            DevTask updated = devTaskService.updateTask(id, task);
            result.put("code", 0);
            result.put("data", updated);
            result.put("msg", "任务更新成功");
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "任务更新失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        devTaskService.deleteTask(id);
        result.put("code", 0);
        result.put("msg", "任务删除成功");
        return result;
    }

    @GetMapping("/{id}/ds-detail")
    public Map<String, Object> dsDetail(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", devTaskService.getDsTaskDetail(id));
        return result;
    }
}
