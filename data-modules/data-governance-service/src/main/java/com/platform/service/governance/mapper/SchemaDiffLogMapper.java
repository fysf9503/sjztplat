package com.platform.service.governance.mapper;

import com.platform.service.governance.entity.SchemaDiffLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchemaDiffLogMapper extends JpaRepository<SchemaDiffLog, String> {
    void deleteByTableSchemaId(String tableSchemaId);
    List<SchemaDiffLog> findByTableSchemaIdOrderByCreateTimeDesc(String tableSchemaId);
    List<SchemaDiffLog> findByTableSchemaIdAndResolvedOrderByCreateTimeDesc(String tableSchemaId, String resolved);
}
