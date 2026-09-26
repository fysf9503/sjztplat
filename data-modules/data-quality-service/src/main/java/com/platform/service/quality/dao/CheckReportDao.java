package com.platform.service.quality.dao;

import com.platform.service.quality.api.entity.CheckReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckReportDao extends JpaRepository<CheckReportEntity, String> {
}
