package com.platform.service.governance.service;

import com.platform.service.governance.entity.SysGroup;
import com.platform.service.governance.entity.SysGroupResource;
import com.platform.service.governance.entity.SysUserGroup;
import com.platform.service.governance.mapper.SysGroupMapper;
import com.platform.service.governance.mapper.SysGroupResourceMapper;
import com.platform.service.governance.mapper.SysUserGroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final SysGroupMapper groupMapper;
    private final SysUserGroupMapper userGroupMapper;
    private final SysGroupResourceMapper resourceMapper;

    public List<SysGroup> listGroups() {
        return groupMapper.findAll();
    }

    public SysGroup getGroup(String id) {
        return groupMapper.findById(id).orElse(null);
    }

    public void updateGroup(SysGroup group) {
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.save(group);
    }

    @Transactional
    public void deleteGroup(String id) {
        userGroupMapper.deleteByGroupId(id);
        resourceMapper.deleteByGroupId(id);
        groupMapper.deleteById(id);
    }

    @Transactional
    public SysGroup createGroup(SysGroup group, String creatorUserId) {
        group.setOwnerId(creatorUserId);
        group.setStatus("1");
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.save(group);

        SysUserGroup userGroup = new SysUserGroup();
        userGroup.setUserId(creatorUserId);
        userGroup.setGroupId(group.getId());
        userGroup.setRoleInGroup("owner");
        userGroup.setCreateTime(LocalDateTime.now());
        userGroup.setUpdateTime(LocalDateTime.now());
        userGroupMapper.save(userGroup);

        return group;
    }

    @Transactional
    public void addMember(String groupId, String userId, String role) {
        if (userGroupMapper.countByGroupIdAndUserId(groupId, userId) > 0) {
            return;
        }
        SysUserGroup userGroup = new SysUserGroup();
        userGroup.setGroupId(groupId);
        userGroup.setUserId(userId);
        userGroup.setRoleInGroup(role != null ? role : "member");
        userGroup.setCreateTime(LocalDateTime.now());
        userGroup.setUpdateTime(LocalDateTime.now());
        userGroupMapper.save(userGroup);
    }

    @Transactional
    public void removeMember(String groupId, String userId) {
        userGroupMapper.deleteByGroupIdAndUserId(groupId, userId);
    }

    public List<SysUserGroup> getGroupMembers(String groupId) {
        return userGroupMapper.findByGroupId(groupId);
    }

    public List<String> getUserGroups(String userId) {
        return userGroupMapper.findByUserId(userId).stream()
                .map(SysUserGroup::getGroupId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void bindResource(String groupId, String resourceType, String resourceId, String permission) {
        SysGroupResource resource = new SysGroupResource();
        resource.setGroupId(groupId);
        resource.setResourceType(resourceType);
        resource.setResourceId(resourceId);
        resource.setPermission(permission != null ? permission : "read");
        resource.setCreateTime(LocalDateTime.now());
        resource.setUpdateTime(LocalDateTime.now());
        resourceMapper.save(resource);
    }

    @Transactional
    public void unbindResource(String groupId, String resourceType, String resourceId) {
        resourceMapper.deleteByGroupIdAndResourceTypeAndResourceId(groupId, resourceType, resourceId);
    }

    public List<SysGroupResource> getGroupResources(String groupId) {
        return resourceMapper.findByGroupId(groupId);
    }

    public boolean hasPermission(String userId, String resourceType, String resourceId, String requiredPermission) {
        List<String> groupIds = getUserGroups(userId);
        if (groupIds.isEmpty()) return false;

        List<SysGroupResource> resources = resourceMapper.findByGroupIdInAndResourceTypeAndResourceId(groupIds, resourceType, resourceId);
        if (resources.isEmpty()) return false;

        return resources.stream().anyMatch(r -> hasPermissionLevel(r.getPermission(), requiredPermission));
    }

    private boolean hasPermissionLevel(String userPermission, String requiredPermission) {
        int userLevel = permissionLevel(userPermission);
        int requiredLevel = permissionLevel(requiredPermission);
        return userLevel >= requiredLevel;
    }

    private int permissionLevel(String permission) {
        return switch (permission == null ? "read" : permission) {
            case "admin" -> 3;
            case "write" -> 2;
            case "read" -> 1;
            default -> 0;
        };
    }
}
