package com.platform.service.system.mapper;

import com.platform.service.system.entity.DevWorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevWorkflowTaskMapper extends JpaRepository<DevWorkflowTask, String> {
    List<DevWorkflowTask> findByWorkflowId(String workflowId);
    List<DevWorkflowTask> findByTaskId(String taskId);
    void deleteByWorkflowId(String workflowId);
}
