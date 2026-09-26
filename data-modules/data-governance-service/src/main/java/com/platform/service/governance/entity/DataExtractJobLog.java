package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_extract_job_log")
public class DataExtractJobLog {
    @Id
    @Column(name = "id")
    private String id;
    private String jobId;
    private String triggerType;
    private String status;
    private Long affectedRows;
    private String errorMsg;
    private String executeBatch;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
}
