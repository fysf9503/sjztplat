package com.platform.service.lineage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.platform")
public class LineageApplication {
    public static void main(String[] args) {
        SpringApplication.run(LineageApplication.class, args);
    }
}
