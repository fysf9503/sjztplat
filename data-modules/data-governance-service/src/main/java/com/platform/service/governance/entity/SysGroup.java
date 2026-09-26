package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_group")
public class SysGroup {
    @Id
    @Column(name = "id")
    private String id;
    private String groupName;
    private String groupCode;
    private String description;
    private String ownerId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
