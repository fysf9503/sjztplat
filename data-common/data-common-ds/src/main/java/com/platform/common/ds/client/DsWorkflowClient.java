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
public class DsWorkflowClient {

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
        return props.getApiUrl() + path;
    }

    // ===== 任务定义管理 =====

    /**
     * 创建 DS 任务定义，返回任务 code
     */
    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> createTaskDefinition(String name, String taskType,
                                                                String taskParamsJson, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("taskType", taskType);
        body.put("taskParams", taskParamsJson);
        body.put("description", description != null ? description : "");
        body.put("flag", "YES");
        body.put("taskPriority", "MEDIUM");
        body.put("workerGroup", props.getDefaultWorkerGroup());
        body.put("failRetryTimes", 0);
        body.put("failRetryInterval", 1);
        body.put("timeoutFlag", "CLOSE");
        body.put("timeoutNotifyStrategy", "");
        body.put("timeout", 0);
        body.put("delayTime", 0);
        body.put("environmentCode", 0);
        body.put("taskGroupId", 0);
        body.put("taskGroupPriority", 0);
        body.put("tenantCode", props.getDefaultTenant());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/projects/" + props.getProjectCode() + "/task-definitions"),
                    entity, Map.class);
            DsResult<Map<String, Object>> result = toResult(resp.getBody());
            return result;
        } catch (Exception e) {
            log.error("DS 创建任务定义失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    /**
     * 更新 DS 任务定义
     */
    public DsResult<Void> updateTaskDefinition(Long taskCode, String name, String taskType,
                                                String taskParamsJson, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("taskType", taskType);
        body.put("taskParams", taskParamsJson);
        body.put("description", description != null ? description : "");
        body.put("flag", "YES");
        body.put("taskPriority", "MEDIUM");
        body.put("workerGroup", props.getDefaultWorkerGroup());
        body.put("failRetryTimes", 0);
        body.put("failRetryInterval", 1);
        body.put("timeoutFlag", "CLOSE");
        body.put("timeout", 0);
        body.put("delayTime", 0);
        body.put("tenantCode", props.getDefaultTenant());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            restTemplate.put(
                    url("/projects/" + props.getProjectCode() + "/task-definitions/" + taskCode),
                    entity);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DS 更新任务定义失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    /**
     * 查询 DS 任务定义（含 code）
     */
    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> getTaskDefinition(Long taskCode) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/projects/" + props.getProjectCode() + "/task-definitions/" + taskCode),
                    HttpMethod.GET, entity, Map.class);
            return toResult(resp.getBody());
        } catch (Exception e) {
            log.error("DS 查询任务定义失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    /**
     * 删除 DS 任务定义
     */
    public DsResult<Void> deleteTaskDefinition(Long taskCode) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            restTemplate.exchange(
                    url("/projects/" + props.getProjectCode() + "/task-definitions/" + taskCode),
                    HttpMethod.DELETE, entity, Void.class);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DS 删除任务定义失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    // ===== 带 DAG 依赖关系的工作流管理 =====

    /**
     * 创建带 DAG 依赖关系的工作流
     * @param taskDefinitionJson 任务定义 JSON 数组字符串
     * @param taskRelationJson 任务关系 JSON 数组字符串（描述依赖边）
     */
    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> createWorkflowWithDag(String name, String description,
                                                                 String taskDefinitionJson,
                                                                 String taskRelationJson) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description != null ? description : "");
        body.put("taskDefinitionJsonObj", taskDefinitionJson);
        body.put("taskRelationJson", taskRelationJson);
        body.put("processIsParallel", false);
        body.put("failureStrategy", "CONTINUE");
        body.put("warningType", "ALL");
        body.put("warningGroupId", 0);
        body.put("executionType", "PARALLEL");
        body.put("workerGroup", props.getDefaultWorkerGroup());
        body.put("tenantCode", props.getDefaultTenant());
        body.put("timeout", 0);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions"),
                    entity, Map.class);
            return toResult(resp.getBody());
        } catch (Exception e) {
            log.error("DS 创建 DAG 工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    /**
     * 更新带 DAG 依赖关系的工作流
     */
    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> updateWorkflowWithDag(Long workflowCode, String name,
                                                                 String description,
                                                                 String taskDefinitionJson,
                                                                 String taskRelationJson) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description != null ? description : "");
        body.put("taskDefinitionJsonObj", taskDefinitionJson);
        body.put("taskRelationJson", taskRelationJson);
        body.put("processIsParallel", false);
        body.put("failureStrategy", "CONTINUE");
        body.put("warningType", "ALL");
        body.put("warningGroupId", 0);
        body.put("executionType", "PARALLEL");
        body.put("workerGroup", props.getDefaultWorkerGroup());
        body.put("tenantCode", props.getDefaultTenant());
        body.put("timeout", 0);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            restTemplate.put(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions/" + workflowCode),
                    entity);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DS 更新 DAG 工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> createWorkflow(String name, String description,
                                                          String taskDefinitionJsonObj) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("taskDefinitionJsonObj", taskDefinitionJsonObj);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions"),
                    entity, Map.class);
            return toResult(resp.getBody());
        } catch (Exception e) {
            log.error("DolphinScheduler 创建工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> updateWorkflow(Long workflowCode, String name,
                                                          String taskDefinitionJsonObj) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("taskDefinitionJsonObj", taskDefinitionJsonObj);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            restTemplate.put(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions/" + workflowCode),
                    entity);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DolphinScheduler 更新工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> getWorkflow(Long workflowCode) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions/" + workflowCode),
                    HttpMethod.GET, entity, Map.class);
            return toResult(resp.getBody());
        } catch (Exception e) {
            log.error("DolphinScheduler 查询工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public DsResult<Map<String, Object>> listWorkflows(int pageNo, int pageSize) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions?pageNo=" + pageNo + "&pageSize=" + pageSize),
                    HttpMethod.GET, entity, Map.class);
            return toResult(resp.getBody());
        } catch (Exception e) {
            log.error("DolphinScheduler 查询工作流列表失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    public DsResult<Void> deleteWorkflow(Long workflowCode) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            restTemplate.exchange(
                    url("/projects/" + props.getProjectCode() + "/workflow-definitions/" + workflowCode),
                    HttpMethod.DELETE, entity, Void.class);
            return DsResult.ok();
        } catch (Exception e) {
            log.error("DolphinScheduler 删除工作流失败: {}", e.getMessage());
            return DsResult.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private DsResult<Map<String, Object>> toResult(Map body) {
        DsResult<Map<String, Object>> result = new DsResult<>();
        if (body != null) {
            result.setCode(((Number) body.getOrDefault("code", -1)).intValue());
            result.setMsg((String) body.get("msg"));
            result.setData((Map<String, Object>) body.get("data"));
        }
        return result;
    }
}
