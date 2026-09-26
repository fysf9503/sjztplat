package com.platform.service.quality.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "quality_check_report")
public class CheckReportEntity implements Serializable {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "check_rule_id")
    private String checkRuleId;

    @Column(name = "check_date")
    private LocalDateTime checkDate;

    @Column(name = "check_error_count")
    private Integer checkErrorCount;

    @Column(name = "check_total_count")
    private Integer checkTotalCount;

    @Column(name = "check_result")
    private String checkResult;

    @Column(name = "check_batch")
    private String checkBatch;
}
