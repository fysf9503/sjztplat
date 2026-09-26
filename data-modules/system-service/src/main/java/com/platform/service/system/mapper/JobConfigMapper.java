package com.platform.service.system.mapper;

import com.platform.service.system.entity.JobConfig;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobConfigMapper extends JpaRepository<JobConfig, String> {

    List<JobConfig> findByModule(String module, Sort sort);
}
