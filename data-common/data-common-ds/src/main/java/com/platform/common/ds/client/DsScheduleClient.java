package com.platform.common.ds.client;

import com.platform.common.ds.config.DolphinSchedulerProperties;
import com.platform.common.ds.model.DsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DsScheduleClient {

    private final DolphinSchedulerProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (props.getToken() != null) {
            headers.add("token", props.getToken());
        }
        return headers;
    }

    private String url(String path) {
        return props.getApiUrl() + "/projects/" + props.getProjectCode() + path;
    }

    @SuppressWarnings("unchecked")
    public DsResult<Long> createSchedule(Long workflowDefinitionCode, String crontab,
                                         String startTime, String endTime) {
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("startTime", startTime);
        schedule.put("endTime", endTime);
        schedule.put("timezoneId", "Asia/Shanghai");
        schedule.put("crontab", crontab);

        Map<String, Object> body = new HashMap<>();
        body.put("workflowDefinitionCode", workflowDefinitionCode);
        body.put("schedule", schedule);
        body.put("warningType", "ALL");
        body.put("failureStrategy", "CONTINUE");
        body.put("workerGroup", props.getDefaultWorkerGroup());
        body.put("tenantCode", props.getDefaultTenant());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url("/schedules"), entity, Map.class);
            DsResult<Long> result = new DsResult<>();
            if (resp.getBody() != null) {
                result.setCode(((Number) resp.getBody().getOrDefault("code", -1)).intValue());
                result.setMsg((String) resp.getBody().get("msg"));
                Object id = resp.getBody().get("data");
                if (id instanceof Number n) result.setData(n.longValue());
            }
            return result;
        } catch (Exception e) {
            log.error("DolphinScheduler 创建调度失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    public DsResult<Void> onlineSchedule(Long scheduleId) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            restTemplate.postForEntity(url("/schedules/" + scheduleId + "/online"), entity, Void.class);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DolphinScheduler 上线调度失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    public DsResult<Void> offlineSchedule(Long scheduleId) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            restTemplate.postForEntity(url("/schedules/" + scheduleId + "/offline"), entity, Void.class);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DolphinScheduler 下线调度失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }
}
