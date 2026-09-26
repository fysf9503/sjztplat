package com.platform.service.lineage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "data_lineage_field")
public class DataLineageField {
    @Id
    @Column(name = "id")
    private String id;
    private String lineageId;
    private String sourceDatabase;
    private String sourceTable;
    private String sourceField;
    private String targetDatabase;
    private String targetTable;
    private String targetField;
    private String transformExpr;
    private String fieldType;
}
