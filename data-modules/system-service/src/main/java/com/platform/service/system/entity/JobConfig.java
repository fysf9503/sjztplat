package com.platform.service.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_job_config")
public class JobConfig {

    @Id
    @Column(name = "id")
    private String id;
    private String jobName;
    private String handler;
    private String module;
    private String cronExpr;
    private String jobParam;
    private String description;
    private Long dsWorkflowCode;
    private Long dsScheduleId;
    private String status;
    private String groupId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
