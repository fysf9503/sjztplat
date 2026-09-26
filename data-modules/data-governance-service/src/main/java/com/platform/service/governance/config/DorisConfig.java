package com.platform.service.governance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Data
@Configuration
@ConfigurationProperties(prefix = "doris")
public class DorisConfig {
    private String feHost;
    private int feQueryPort;
    private int feHttpPort;
    private String username;
    private String password;
    private String database;

    @Bean
    public JdbcTemplate dorisJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
