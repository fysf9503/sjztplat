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
@Table(name = "quality_schedule_log")
public class ScheduleLogEntity implements Serializable {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "status")
    private String status;

    @Column(name = "execute_job_id")
    private String executeJobId;

    @Column(name = "execute_rule_id")
    private String executeRuleId;

    @Column(name = "execute_date")
    private LocalDateTime executeDate;

    @Column(name = "execute_result")
    private String executeResult;

    @Column(name = "execute_batch")
    private String executeBatch;
}
