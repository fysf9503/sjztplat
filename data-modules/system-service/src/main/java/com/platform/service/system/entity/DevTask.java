package com.platform.service.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开发任务定义
 * 每个任务对应 DS 中的一个 task-definition
 * 通过 sourceTables / targetTables 自动推导任务间依赖关系
 */
@Data
@Entity
@Table(name = "dev_task")
public class DevTask {

    @Id
    @Column(name = "id")
    private String id;

    /** 任务名称（唯一） */
    private String taskName;

    /** 任务类型: FLINK, SQL, SHELL, PYTHON, CUSTOM */
    private String taskType;

    /** 任务参数 JSON（DS taskParams 格式） */
    @Column(columnDefinition = "TEXT")
    private String taskParams;

    /** 描述 */
    private String description;

    /** 源表 FQN JSON 数组（任务读取的表，用于自动推导上游依赖） */
    @Column(columnDefinition = "TEXT")
    private String sourceTables;

    /** 目标表 FQN JSON 数组（任务写入的表，用于自动推导下游依赖） */
    @Column(columnDefinition = "TEXT")
    private String targetTables;

    /** DS 任务定义 code */
    private Long dsTaskCode;

    /** DS 任务定义版本号 */
    private Integer dsTaskVersion;

    /** DRAFT, PUBLISHED */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
