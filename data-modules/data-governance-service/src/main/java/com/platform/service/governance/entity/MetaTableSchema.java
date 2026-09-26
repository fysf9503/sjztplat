package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "meta_table_schema")
public class MetaTableSchema {
    @Id
    @Column(name = "id")
    private String id;
    private String groupId;
    private String sourceCatalog;
    private String sourceDatabase;
    private String sourceTable;
    private String targetDatabase;
    private String targetTable;
    private String schemaJson;
    private Integer columnCount;
    private LocalDateTime lastSyncTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
