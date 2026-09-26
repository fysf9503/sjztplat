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
public class DsExecutorClient {

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
    public DsResult<Long> startWorkflow(Long workflowDefinitionCode, String scheduleTime) {
        Map<String, Object> body = new HashMap<>();
        body.put("workflowDefinitionCode", workflowDefinitionCode);
        body.put("scheduleTime", scheduleTime);
        body.put("failureStrategy", "END");
        body.put("taskDependType", "NONE");
        body.put("execType", "EXECUTE");
        body.put("warningType", "ALL");
        body.put("runMode", "NORMAL");
        body.put("workflowInstancePriority", "MEDIUM");
        body.put("workerGroup", props.getDefaultWorkerGroup());
        body.put("tenantCode", props.getDefaultTenant());
        body.put("dryRun", 0);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/executors/start-workflow-instance"), entity, Map.class);
            DsResult<Long> result = new DsResult<>();
            if (resp.getBody() != null) {
                result.setCode(((Number) resp.getBody().getOrDefault("code", -1)).intValue());
                result.setMsg((String) resp.getBody().get("msg"));
                Object data = resp.getBody().get("data");
                if (data instanceof Number n) result.setData(n.longValue());
            }
            return result;
        } catch (Exception e) {
            log.error("DolphinScheduler 启动工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    public DsResult<Void> executeAction(Long workflowInstanceId, String executeType) {
        Map<String, Object> body = new HashMap<>();
        body.put("workflowInstanceId", workflowInstanceId);
        body.put("executeType", executeType);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            restTemplate.postForEntity(url("/executors/execute"), entity, Void.class);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DolphinScheduler 操作工作流实例失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    public DsResult<Void> stopWorkflow(Long workflowInstanceId) {
        return executeAction(workflowInstanceId, "STOP");
    }

    public DsResult<Void> retryFailedTasks(Long workflowInstanceId) {
        return executeAction(workflowInstanceId, "START_FAILURE_TASK_PROCESS");
    }
}
