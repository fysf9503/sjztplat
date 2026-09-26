package com.platform.common.om.client;

import com.platform.common.om.config.OpenMetadataProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OmTableClient {

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
    public Map<String, Object> createOrUpdateTable(Map<String, Object> tableBody) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(tableBody, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/v1/tables"), HttpMethod.PUT, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("OpenMetadata 创建/更新表元数据失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTableByFqn(String fqn, String fields) {
        String path = "/v1/tables/name/" + fqn;
        if (fields != null && !fields.isBlank()) {
            path += "?fields=" + fields;
        }
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url(path), HttpMethod.GET, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("OpenMetadata 查询表失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> listTables(int limit, int offset) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/v1/tables?limit=" + limit + "&offset=" + offset),
                    HttpMethod.GET, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("OpenMetadata 查询表列表失败: {}", e.getMessage());
            return null;
        }
    }
}
