package com.platform.service.governance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user_group")
public class SysUserGroup {
    @Id
    @Column(name = "id")
    private String id;
    private String userId;
    private String groupId;
    private String roleInGroup;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
