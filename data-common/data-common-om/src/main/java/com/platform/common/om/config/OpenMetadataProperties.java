package com.platform.common.om.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openmetadata")
public class OpenMetadataProperties {

    private String apiUrl = "http://localhost:8585/api";
    private String jwtToken;
    private String webhookSecret;
}
