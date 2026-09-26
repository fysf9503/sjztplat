package com.platform.service.quality.api.entity;

import com.platform.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "quality_check_rule")
public class CheckRuleEntity extends BaseEntity {

    @Column(name = "rule_name")
    private String ruleName;
    @Column(name = "rule_type")
    private String ruleType;
    @Column(name = "rule_level")
    private String ruleLevel;
    @Column(name = "rule_source_id")
    private String ruleSourceId;
    @Column(name = "rule_source_name")
    private String ruleSourceName;
    @Column(name = "rule_sql")
    private String ruleSql;
    @Column(name = "last_check_batch")
    private String lastCheckBatch;
}
