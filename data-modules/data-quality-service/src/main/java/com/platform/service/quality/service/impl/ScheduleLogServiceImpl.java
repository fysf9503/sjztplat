package com.platform.service.quality.service.impl;

import com.platform.service.quality.api.entity.ScheduleLogEntity;
import com.platform.service.quality.dao.ScheduleLogDao;
import com.platform.service.quality.service.ScheduleLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleLogServiceImpl implements ScheduleLogService {

    private final ScheduleLogDao scheduleLogDao;

    @Override
    public List<ScheduleLogEntity> list() {
        return scheduleLogDao.findAll();
    }

    @Override
    public ScheduleLogEntity getById(String id) {
        return scheduleLogDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public ScheduleLogEntity save(ScheduleLogEntity entity) {
        return scheduleLogDao.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        scheduleLogDao.deleteById(id);
    }

    @Override
    public Page<ScheduleLogEntity> page(Pageable pageable) {
        return scheduleLogDao.findAll(pageable);
    }
}
