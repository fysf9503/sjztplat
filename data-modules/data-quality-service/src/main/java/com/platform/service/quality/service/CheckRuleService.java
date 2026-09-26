package com.platform.service.quality.service;

import com.platform.service.quality.api.entity.CheckRuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CheckRuleService {
    List<CheckRuleEntity> listEnabledRules();
    List<CheckRuleEntity> list();
    CheckRuleEntity getById(String id);
    CheckRuleEntity save(CheckRuleEntity entity);
    void deleteById(String id);
    Page<CheckRuleEntity> page(Pageable pageable);
}
