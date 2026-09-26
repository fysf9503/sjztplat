package com.platform.service.governance.mapper;

import com.platform.service.governance.entity.SysUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysUserGroupMapper extends JpaRepository<SysUserGroup, String> {
    List<SysUserGroup> findByGroupId(String groupId);
    List<SysUserGroup> findByUserId(String userId);
    long countByGroupIdAndUserId(String groupId, String userId);
    void deleteByGroupId(String groupId);
    void deleteByGroupIdAndUserId(String groupId, String userId);
}
