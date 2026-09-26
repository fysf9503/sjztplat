package com.platform.service.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开发工作流
 * 将多个 DevTask 组织成一个 DAG，对应 DS 中的一个 workflow-definition
 */
@Data
@Entity
@Table(name = "dev_workflow")
public class DevWorkflow {

    @Id
    @Column(name = "id")
    private String id;

    /** 工作流名称 */
    private String workflowName;

    /** 描述 */
    private String description;

    /** Cron 调度表达式 */
    private String cronExpr;

    /** DS 工作流 code */
    private Long dsWorkflowCode;

    /** DS 调度 ID */
    private Long dsScheduleId;

    /** DRAFT, PUBLISHED, ONLINE, OFFLINE */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
