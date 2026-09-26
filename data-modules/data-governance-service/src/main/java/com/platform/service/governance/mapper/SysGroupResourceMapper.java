package com.platform.service.governance.mapper;

import com.platform.service.governance.entity.SysGroupResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SysGroupResourceMapper extends JpaRepository<SysGroupResource, String> {
    List<SysGroupResource> findByGroupId(String groupId);
    List<SysGroupResource> findByGroupIdInAndResourceTypeAndResourceId(Collection<String> groupIds, String resourceType, String resourceId);
    void deleteByGroupId(String groupId);
    void deleteByGroupIdAndResourceTypeAndResourceId(String groupId, String resourceType, String resourceId);
}
