package com.platform.service.lineage.mapper;

import com.platform.service.lineage.entity.DataLineage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataLineageMapper extends JpaRepository<DataLineage, String> {
    List<DataLineage> findByGroupIdOrderByCreateTimeDesc(String groupId);
}
