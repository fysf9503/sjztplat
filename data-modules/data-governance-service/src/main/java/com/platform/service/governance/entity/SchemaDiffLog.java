package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schema_diff_log")
public class SchemaDiffLog {
    @Id
    @Column(name = "id")
    private String id;
    private String tableSchemaId;
    private String diffType;
    private String columnName;
    private String oldType;
    private String newType;
    private String oldComment;
    private String newComment;
    private String diffDetail;
    private String resolved;
    private String resolvedBy;
    private LocalDateTime resolvedTime;
    private LocalDateTime createTime;
}
