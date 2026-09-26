package com.platform.service.governance.mapper;

import com.platform.service.governance.entity.MetaTableSchema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaTableSchemaMapper extends JpaRepository<MetaTableSchema, String> {
    List<MetaTableSchema> findByStatus(String status);
    List<MetaTableSchema> findByStatusAndGroupId(String status, String groupId);
    List<MetaTableSchema> findByGroupId(String groupId);
}
