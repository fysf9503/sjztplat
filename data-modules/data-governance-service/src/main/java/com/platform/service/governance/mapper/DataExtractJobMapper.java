package com.platform.service.governance.mapper;

import com.platform.service.governance.entity.DataExtractJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataExtractJobMapper extends JpaRepository<DataExtractJob, String> {
    List<DataExtractJob> findByGroupIdOrderByCreateTimeDesc(String groupId);
    List<DataExtractJob> findByStatus(String status);
}
