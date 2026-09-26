package com.platform.service.governance.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.platform.service.governance.entity.DataExtractJob;
import com.platform.service.governance.entity.DataExtractJobLog;
import com.platform.service.governance.mapper.DataExtractJobLogMapper;
import com.platform.service.governance.mapper.DataExtractJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractJobService {

    private final DataExtractJobMapper jobMapper;
    private final DataExtractJobLogMapper logMapper;
    private final JdbcTemplate dorisJdbcTemplate;

    public List<DataExtractJob> listJobs(String groupId) {
        if (groupId != null && !groupId.isBlank()) {
            return jobMapper.findByGroupIdOrderByCreateTimeDesc(groupId);
        }
        return jobMapper.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
    }

    public DataExtractJob getJob(String id) {
        return jobMapper.findById(id).orElse(null);
    }

    @Transactional
    public DataExtractJob createJob(DataExtractJob job) {
        job.setStatus("1");
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        jobMapper.save(job);

        return job;
    }

    @Transactional
    public DataExtractJob updateJob(DataExtractJob job) {
        job.setUpdateTime(LocalDateTime.now());
        jobMapper.save(job);

        return job;
    }

    @Transactional
    public void deleteJob(String id) {
        jobMapper.deleteById(id);
    }

    public DataExtractJobLog executeJob(String jobId, String triggerType) {
        DataExtractJob job = jobMapper.findById(jobId).orElse(null);
        if (job == null) {
            throw new RuntimeException("抽取任务不存在: " + jobId);
        }
        if (!"1".equals(job.getStatus())) {
            throw new RuntimeException("任务已停用: " + job.getJobName());
        }

        String batch = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN);
        DataExtractJobLog jobLog = new DataExtractJobLog();
        jobLog.setJobId(jobId);
        jobLog.setTriggerType(triggerType != null ? triggerType : "manual");
        jobLog.setStatus("2");
        jobLog.setExecuteBatch(batch);
        jobLog.setStartTime(LocalDateTime.now());
        logMapper.save(jobLog);

        try {
            String sql = buildExtractSql(job);
            log.info("执行抽取SQL: {}", sql);

            int affected = dorisJdbcTemplate.update(sql);

            if ("incremental".equals(job.getExtractType()) && job.getIncrementField() != null) {
                updateIncrementWatermark(job);
            }

            jobLog.setStatus("1");
            jobLog.setAffectedRows((long) affected);
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setDurationMs(java.time.Duration.between(jobLog.getStartTime(), jobLog.getEndTime()).toMillis());
            logMapper.save(jobLog);

            log.info("抽取任务完成: {} 影响行数: {}", job.getJobName(), affected);
            return jobLog;

        } catch (Exception e) {
            log.error("抽取任务失败: {}", job.getJobName(), e);
            jobLog.setStatus("0");
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setDurationMs(java.time.Duration.between(jobLog.getStartTime(), jobLog.getEndTime()).toMillis());
            jobLog.setErrorMsg(e.getMessage().length() > 1000 ? e.getMessage().substring(0, 1000) : e.getMessage());
            logMapper.save(jobLog);
            throw new RuntimeException("抽取失败: " + e.getMessage(), e);
        }
    }

    private String buildExtractSql(DataExtractJob job) {
        String sourceTable = buildFullTableName(job.getSourceCatalog(), job.getSourceDatabase(), job.getSourceTable());
        String targetTable = job.getTargetDatabase() + "." + job.getTargetTable();

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(targetTable);

        if (job.getFieldMapping() != null && !job.getFieldMapping().isBlank() && !job.getFieldMapping().equals("[]")) {
            String cols = parseTargetColumns(job.getFieldMapping());
            if (!cols.isEmpty()) {
                sql.append(" (").append(cols).append(")");
            }
        }

        sql.append(" SELECT ");

        if (job.getFieldMapping() != null && !job.getFieldMapping().isBlank() && !job.getFieldMapping().equals("[]")) {
            String selectCols = parseSourceColumns(job.getFieldMapping());
            sql.append(selectCols);
        } else {
            sql.append("*");
        }

        sql.append(" FROM ").append(sourceTable);

        StringBuilder whereClause = new StringBuilder();

        if ("incremental".equals(job.getExtractType()) && job.getIncrementField() != null) {
            whereClause.append(job.getIncrementField()).append(" > '").append(job.getIncrementValue() != null ? job.getIncrementValue() : "").append("'");
        }

        if (job.getWhereCondition() != null && !job.getWhereCondition().isBlank()) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" AND ");
            }
            whereClause.append("(").append(job.getWhereCondition()).append(")");
        }

        if (!whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        return sql.toString();
    }

    private void updateIncrementWatermark(DataExtractJob job) {
        String sql = String.format("SELECT MAX(%s) FROM %s.%s",
                job.getIncrementField(), job.getTargetDatabase(), job.getTargetTable());
        try {
            String maxValue = dorisJdbcTemplate.queryForObject(sql, String.class);
            if (maxValue != null) {
                job.setIncrementValue(maxValue);
                job.setUpdateTime(LocalDateTime.now());
                jobMapper.save(job);
                log.info("更新增量水位线: {} = {}", job.getIncrementField(), maxValue);
            }
        } catch (Exception e) {
            log.warn("更新增量水位线失败: {}", e.getMessage());
        }
    }

    private String buildFullTableName(String catalog, String database, String table) {
        StringBuilder sb = new StringBuilder();
        if (catalog != null && !catalog.isBlank()) {
            sb.append(catalog).append(".");
        }
        if (database != null && !database.isBlank()) {
            sb.append(database).append(".");
        }
        sb.append(table);
        return sb.toString();
    }

    private String parseTargetColumns(String fieldMappingJson) {
        return fieldMappingJson.replaceAll("[\\[\\]{}\"]", "")
                .replaceAll("\\{", "")
                .replaceAll("target:", "")
                .replaceAll("source:", "")
                .replaceAll(",", ", ");
    }

    private String parseSourceColumns(String fieldMappingJson) {
        return fieldMappingJson.replaceAll("[\\[\\]{}\"]", "")
                .replaceAll("\\{", "")
                .replaceAll("target:", "")
                .replaceAll("source:", "")
                .replaceAll(",", ", ");
    }

    public List<DataExtractJobLog> getJobLogs(String jobId) {
        return logMapper.findByJobIdOrderByStartTimeDesc(jobId, PageRequest.of(0, 50));
    }
}
