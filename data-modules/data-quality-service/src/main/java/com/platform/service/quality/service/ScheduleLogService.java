package com.platform.service.quality.service;

import com.platform.service.quality.api.entity.ScheduleLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScheduleLogService {
    List<ScheduleLogEntity> list();
    ScheduleLogEntity getById(String id);
    ScheduleLogEntity save(ScheduleLogEntity entity);
    void deleteById(String id);
    Page<ScheduleLogEntity> page(Pageable pageable);
}
