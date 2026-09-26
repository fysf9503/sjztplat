package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_extract_job")
public class DataExtractJob {
    @Id
    @Column(name = "id")
    private String id;
    private String jobName;
    private String groupId;
    private String sourceType;
    private String sourceCatalog;
    private String sourceDatabase;
    private String sourceTable;
    private String targetDatabase;
    private String targetTable;
    private String extractType;
    private String incrementField;
    private String incrementValue;
    private String whereCondition;
    private String fieldMapping;
    private String cronExpression;
    private Long dsWorkflowCode;
    private String status;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
