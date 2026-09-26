package com.platform.service.realtime.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "doris")
public class DorisConfig {

    private String feHost = "localhost";
    private int feHttpPort = 8030;
    private int feQueryPort = 9030;
    private String username = "root";
    private String password = "";
    private String database = "data_platform";

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%%2B8",
                feHost, feQueryPort, database);
    }

    public String getStreamLoadUrl(String table) {
        return String.format("http://%s:%d/api/%s/%s/_stream_load",
                feHost, feHttpPort, database, table);
    }
}
