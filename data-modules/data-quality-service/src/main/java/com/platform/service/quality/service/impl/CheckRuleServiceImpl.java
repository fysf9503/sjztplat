package com.platform.service.quality.service.impl;

import com.platform.service.quality.api.entity.CheckRuleEntity;
import com.platform.service.quality.dao.CheckRuleDao;
import com.platform.service.quality.service.CheckRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CheckRuleServiceImpl implements CheckRuleService {

    private final CheckRuleDao checkRuleDao;

    @Override
    public List<CheckRuleEntity> listEnabledRules() {
        return checkRuleDao.findByStatusOrderByCreateTimeDesc("1");
    }

    @Override
    public List<CheckRuleEntity> list() {
        return checkRuleDao.findAll();
    }

    @Override
    public CheckRuleEntity getById(String id) {
        return checkRuleDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public CheckRuleEntity save(CheckRuleEntity entity) {
        return checkRuleDao.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        checkRuleDao.deleteById(id);
    }

    @Override
    public Page<CheckRuleEntity> page(Pageable pageable) {
        return checkRuleDao.findAll(pageable);
    }
}
