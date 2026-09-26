package com.platform.service.governance.handler;

import com.platform.service.governance.entity.DataExtractJob;
import com.platform.service.governance.mapper.DataExtractJobMapper;
import com.platform.service.governance.service.ExtractJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class DynamicExtractHandler {

    private final DataExtractJobMapper jobMapper;
    private final ExtractJobService extractJobService;

    @PostMapping("/dynamicExtract")
    public void dynamicExtractHandler(@RequestParam(value = "param", required = false) String param) {
        log.info("动态数据抽取任务 - 参数: {}", param);

        if (param != null && !param.isBlank()) {
            extractJobService.executeJob(param.trim(), "schedule");
            return;
        }

        List<DataExtractJob> jobs = jobMapper.findByStatus("1");

        log.info("待执行抽取任务数: {}", jobs.size());
        for (DataExtractJob job : jobs) {
            try {
                extractJobService.executeJob(job.getId(), "schedule");
            } catch (Exception e) {
                log.error("任务执行失败: {}", job.getJobName(), e);
            }
        }
    }
}
