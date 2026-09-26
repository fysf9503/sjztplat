package com.platform.common.ds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dolphinscheduler")
public class DolphinSchedulerProperties {

    private String apiUrl = "http://localhost:12345/dolphinscheduler/api";
    private String token;
    private Long projectCode = 1L;
    private String defaultWorkerGroup = "default";
    private String defaultTenant = "default";
}
