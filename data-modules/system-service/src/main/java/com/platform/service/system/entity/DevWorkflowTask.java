package com.platform.service.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流-任务关联表
 * 记录每个工作流包含哪些任务，以及每个任务的上游依赖（手动指定或自动推导）
 */
@Data
@Entity
@Table(name = "dev_workflow_task")
public class DevWorkflowTask {

    @Id
    @Column(name = "id")
    private String id;

    /** 所属工作流 ID */
    private String workflowId;

    /** 关联的任务 ID */
    private String taskId;

    /** DS 任务 code */
    private Long dsTaskCode;

    /**
     * 上游任务 ID JSON 数组
     * AUTO 表示由系统根据表血缘自动推导
     * 空数组表示无上游依赖（起始节点）
     */
    @Column(columnDefinition = "TEXT")
    private String upstreamTaskIds;

    /** AUTO(自动推导待确认), MANUAL(手动添加), CONFIRMED(已确认) */
    private String dependencyType;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
