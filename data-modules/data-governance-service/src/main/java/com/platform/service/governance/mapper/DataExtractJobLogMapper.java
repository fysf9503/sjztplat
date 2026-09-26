package com.platform.service.governance.mapper;

import com.platform.service.governance.entity.DataExtractJobLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataExtractJobLogMapper extends JpaRepository<DataExtractJobLog, String> {
    List<DataExtractJobLog> findByJobIdOrderByStartTimeDesc(String jobId);
    List<DataExtractJobLog> findByJobIdOrderByStartTimeDesc(String jobId, Pageable pageable);
}
