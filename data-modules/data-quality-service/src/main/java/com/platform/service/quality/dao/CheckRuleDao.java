package com.platform.service.quality.dao;

import com.platform.service.quality.api.entity.CheckRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckRuleDao extends JpaRepository<CheckRuleEntity, String> {
    List<CheckRuleEntity> findByStatusOrderByCreateTimeDesc(String status);
}
