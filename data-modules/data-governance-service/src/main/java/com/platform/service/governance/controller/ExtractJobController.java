package com.platform.service.governance.controller;

import com.platform.service.governance.entity.DataExtractJob;
import com.platform.service.governance.service.ExtractJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/governance/extract")
@RequiredArgsConstructor
public class ExtractJobController {

    private final ExtractJobService extractJobService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(value = "groupId", required = false) String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", extractJobService.listJobs(groupId));
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", extractJobService.getJob(id));
        return result;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody DataExtractJob job) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", extractJobService.createJob(job));
        return result;
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody DataExtractJob job) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", extractJobService.updateJob(job));
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable(value = "id") String id) {
        extractJobService.deleteJob(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/{id}/execute")
    public Map<String, Object> execute(@PathVariable(value = "id") String id,
                                        @RequestParam(value = "triggerType", defaultValue = "manual") String triggerType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", extractJobService.executeJob(id, triggerType));
        return result;
    }

    @GetMapping("/{id}/logs")
    public Map<String, Object> logs(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", extractJobService.getJobLogs(id));
        return result;
    }
}
