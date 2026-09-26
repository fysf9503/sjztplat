package com.platform.service.quality.service.impl;

import com.platform.service.quality.api.entity.CheckReportEntity;
import com.platform.service.quality.dao.CheckReportDao;
import com.platform.service.quality.service.CheckReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CheckReportServiceImpl implements CheckReportService {

    private final CheckReportDao checkReportDao;

    @Override
    public List<CheckReportEntity> list() {
        return checkReportDao.findAll();
    }

    @Override
    public CheckReportEntity getById(String id) {
        return checkReportDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public CheckReportEntity save(CheckReportEntity entity) {
        return checkReportDao.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        checkReportDao.deleteById(id);
    }

    @Override
    public Page<CheckReportEntity> page(Pageable pageable) {
        return checkReportDao.findAll(pageable);
    }
}
