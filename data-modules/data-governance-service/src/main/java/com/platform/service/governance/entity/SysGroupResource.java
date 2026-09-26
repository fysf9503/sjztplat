package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_group_resource")
public class SysGroupResource {
    @Id
    @Column(name = "id")
    private String id;
    private String groupId;
    private String resourceType;
    private String resourceId;
    private String permission;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
