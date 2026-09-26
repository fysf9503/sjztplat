package com.platform.service.lineage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_lineage")
public class DataLineage {
    @Id
    @Column(name = "id")
    private String id;
    private String sourceCatalog;
    private String sourceDatabase;
    private String sourceTable;
    private String targetDatabase;
    private String targetTable;
    private String transformSql;
    private String jobId;
    private String jobType;
    private String groupId;
    private LocalDateTime createTime;
}
