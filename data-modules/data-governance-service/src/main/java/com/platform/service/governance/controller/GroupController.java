package com.platform.service.governance.controller;

import com.platform.service.governance.entity.SysGroup;
import com.platform.service.governance.entity.SysGroupResource;
import com.platform.service.governance.entity.SysUserGroup;
import com.platform.service.governance.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/governance/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", groupService.listGroups());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable(value = "id") String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", groupService.getGroup(id));
        return result;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody SysGroup group,
                                       @RequestHeader("X-User-Id") String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", groupService.createGroup(group, userId));
        return result;
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody SysGroup group) {
        groupService.updateGroup(group);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable(value = "id") String id) {
        groupService.deleteGroup(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @GetMapping("/{groupId}/members")
    public Map<String, Object> getMembers(@PathVariable(value = "groupId") String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", groupService.getGroupMembers(groupId));
        return result;
    }

    @PostMapping("/{groupId}/members")
    public Map<String, Object> addMember(@PathVariable(value = "groupId") String groupId,
                                           @RequestBody Map<String, String> body) {
        groupService.addMember(groupId, body.get("userId"), body.get("role"));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public Map<String, Object> removeMember(@PathVariable(value = "groupId") String groupId,
                                             @PathVariable(value = "userId") String userId) {
        groupService.removeMember(groupId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @GetMapping("/{groupId}/resources")
    public Map<String, Object> getResources(@PathVariable(value = "groupId") String groupId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", groupService.getGroupResources(groupId));
        return result;
    }

    @PostMapping("/{groupId}/resources")
    public Map<String, Object> bindResource(@PathVariable(value = "groupId") String groupId,
                                             @RequestBody SysGroupResource resource) {
        resource.setGroupId(groupId);
        groupService.bindResource(groupId, resource.getResourceType(),
                resource.getResourceId(), resource.getPermission());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @DeleteMapping("/{groupId}/resources/{resourceType}/{resourceId}")
    public Map<String, Object> unbindResource(@PathVariable(value = "groupId") String groupId,
                                                @PathVariable(value = "resourceType") String resourceType,
                                                @PathVariable(value = "resourceId") String resourceId) {
        groupService.unbindResource(groupId, resourceType, resourceId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }
}
