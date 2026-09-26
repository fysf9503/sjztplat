package com.platform.service.system.mapper;

import com.platform.service.system.entity.DevWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevWorkflowMapper extends JpaRepository<DevWorkflow, String> {
    List<DevWorkflow> findByStatus(String status);
}
