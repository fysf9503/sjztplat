package com.platform.common.om.client;

import com.platform.common.om.config.OpenMetadataProperties;
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
public class OmLineageClient {

    private final OpenMetadataProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (props.getJwtToken() != null) {
            headers.setBearerAuth(props.getJwtToken());
        }
        return headers;
    }

    private String url(String path) {
        return props.getApiUrl() + path;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> addLineage(String fromId, String fromType, String fromFqn,
                                          String toId, String toType, String toFqn,
                                          String sqlQuery) {
        String fromName = fromFqn.contains(".") ? fromFqn.substring(fromFqn.lastIndexOf('.') + 1) : fromFqn;
        String toName = toFqn.contains(".") ? toFqn.substring(toFqn.lastIndexOf('.') + 1) : toFqn;

        Map<String, Object> fromEntity = new HashMap<>();
        fromEntity.put("id", fromId);
        fromEntity.put("type", fromType);
        fromEntity.put("name", fromName);
        fromEntity.put("fullyQualifiedName", fromFqn);

        Map<String, Object> toEntity = new HashMap<>();
        toEntity.put("id", toId);
        toEntity.put("type", toType);
        toEntity.put("name", toName);
        toEntity.put("fullyQualifiedName", toFqn);

        Map<String, Object> edge = new HashMap<>();
        edge.put("fromEntity", fromEntity);
        edge.put("toEntity", toEntity);

        Map<String, Object> details = new HashMap<>();
        if (sqlQuery != null) {
            details.put("sqlQuery", sqlQuery);
        }
        edge.put("lineageDetails", details);

        Map<String, Object> body = Map.of("edge", edge);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/v1/lineage"), HttpMethod.PUT, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("OpenMetadata 添加血缘失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLineage(String entityType, String id, int upstreamDepth, int downstreamDepth) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/v1/lineage/" + entityType + "/" + id + "?upstreamDepth=" + upstreamDepth + "&downstreamDepth=" + downstreamDepth),
                    HttpMethod.GET, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("OpenMetadata 查询血缘失败: {}", e.getMessage());
            return null;
        }
    }
}
