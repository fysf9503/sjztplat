package com.platform.service.lineage.mapper;

import com.platform.service.lineage.entity.DataLineageField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DataLineageFieldMapper extends JpaRepository<DataLineageField, String> {
    List<DataLineageField> findByLineageId(String lineageId);
    List<DataLineageField> findByLineageIdIn(Collection<String> lineageIds);
    void deleteByLineageId(String lineageId);
}
