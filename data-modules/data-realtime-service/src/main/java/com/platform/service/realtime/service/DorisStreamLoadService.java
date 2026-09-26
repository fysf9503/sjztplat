package com.platform.service.realtime.service;

import com.platform.service.realtime.config.DorisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Doris Stream Load 实时数据导入服务
 *
 * 通过 Doris HTTP Stream Load API 实现实时数据写入，
 * 支持 CSV 和 JSON 两种格式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DorisStreamLoadService {

    private final DorisConfig dorisConfig;

    /**
     * JSON 格式实时写入
     */
    public String streamLoadJson(String table, String jsonData) {
        String url = dorisConfig.getStreamLoadUrl(table);
        String auth = Base64.getEncoder()
                .encodeToString((dorisConfig.getUsername() + ":" + dorisConfig.getPassword())
                        .getBytes(StandardCharsets.UTF_8));

        RestClient.ResponseSpec response = RestClient.create()
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .header("format", "json")
                .header("strip_outer_array", "true")
                .header("fuzzy_parse", "true")
                .header(HttpHeaders.EXPECT, "100-continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonData)
                .retrieve();

        String result = response.body(String.class);
        log.info("Doris Stream Load 完成 - 表: {}", table);
        return result;
    }

    /**
     * CSV 格式批量写入
     */
    public String streamLoadCsv(String table, String csvData, String columns) {
        String url = dorisConfig.getStreamLoadUrl(table);
        String auth = Base64.getEncoder()
                .encodeToString((dorisConfig.getUsername() + ":" + dorisConfig.getPassword())
                        .getBytes(StandardCharsets.UTF_8));

        RestClient.ResponseSpec response = RestClient.create()
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .header("column_separator", ",")
                .header("columns", columns)
                .header(HttpHeaders.EXPECT, "100-continue")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csvData)
                .retrieve();

        String result = response.body(String.class);
        log.info("Doris Stream Load CSV 完成 - 表: {}", table);
        return result;
    }
}
