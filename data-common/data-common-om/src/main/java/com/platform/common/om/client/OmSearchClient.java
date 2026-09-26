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
public class OmSearchClient {

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
    public Map<String, Object> search(String query, String index, int from, int size) {
        String path = "/v1/search/query?q=" + query
                + "&index=" + (index != null ? index : "all")
                + "&from=" + from
                + "&size=" + size;
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url(path), HttpMethod.GET, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("OpenMetadata 搜索失败: {}", e.getMessage());
            return null;
        }
    }
}
