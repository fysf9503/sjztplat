package com.platform.service.quality.service;

import com.platform.service.quality.api.entity.CheckReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CheckReportService {
    List<CheckReportEntity> list();
    CheckReportEntity getById(String id);
    CheckReportEntity save(CheckReportEntity entity);
    void deleteById(String id);
    Page<CheckReportEntity> page(Pageable pageable);
}
