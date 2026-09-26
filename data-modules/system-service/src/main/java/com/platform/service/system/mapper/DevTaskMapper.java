package com.platform.service.system.mapper;

import com.platform.service.system.entity.DevTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevTaskMapper extends JpaRepository<DevTask, String> {
    Optional<DevTask> findByTaskName(String taskName);
    List<DevTask> findByStatus(String status);
    List<DevTask> findByTaskType(String taskType);
    List<DevTask> findByTaskNameContaining(String keyword);
}
