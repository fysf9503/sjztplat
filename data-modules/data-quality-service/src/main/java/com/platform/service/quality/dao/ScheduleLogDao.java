package com.platform.service.quality.dao;

import com.platform.service.quality.api.entity.ScheduleLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleLogDao extends JpaRepository<ScheduleLogEntity, String> {
}
