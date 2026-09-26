package com.platform.service.realtime.handler;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.platform.service.realtime.service.DorisStreamLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 实时任务 Handler
 *
 * 支持两种实时任务模式：
 * 1. 定时微批（Micro-Batch）：DolphinScheduler 按固定间隔触发，从源库拉取增量数据写入 Doris
 * 2. Doris Routine Load：通过 Doris 原生 Routine Load 消费 Kafka 实现实时
 *
 * 【原方案对比】
 * 原项目无实时任务能力，本模块为新增功能。
 * 原 Quartz/TaskScheduler 只能做定时调度，不支持数据流式处理。
 * DolphinScheduler + Doris Stream Load 组合实现了准实时数据同步。
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class RealtimeTaskHandler {

    private final DorisStreamLoadService streamLoadService;
    private final DataSource dataSource;

    /**
     * 定时增量同步：从 MySQL/Doris 源表拉取增量数据，通过 Stream Load 写入 Doris 目标表
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/incrementalSync
     *   - Cron: 0 *\/5 * * * ?  （每5分钟）
     *   - 参数: source_table,target_table,increment_column
     *   示例参数: ods_orders,dws_orders,update_time
     */
    @PostMapping("/incrementalSync")
    public void incrementalSyncHandler(@RequestParam(value = "param", required = false) String param) {
        log.info("增量同步任务开始 - 参数: {}", param);

        if (param == null || param.isBlank()) {
            log.error("参数缺失，格式: source_table,target_table,increment_column");
            return;
        }

        String[] parts = param.split(",");
        if (parts.length < 3) {
            log.error("参数格式错误，应为: source_table,target_table,increment_column");
            return;
        }

        String sourceTable = parts[0];
        String targetTable = parts[1];
        String incrementColumn = parts[2];
        String batch = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // 获取目标表最大增量值
        String maxValSql = String.format("SELECT MAX(%s) FROM %s", incrementColumn, targetTable);
        String maxVal = jdbc.queryForObject(maxValSql, String.class);
        if (maxVal == null) maxVal = "1970-01-01 00:00:00";

        log.info("增量同步 - 源表: {}, 目标表: {}, 增量列: {}, 上次最大值: {}",
                sourceTable, targetTable, incrementColumn, maxVal);

        // 查询增量数据
        String querySql = String.format("SELECT * FROM %s WHERE %s > '%s' ORDER BY %s LIMIT 10000",
                sourceTable, incrementColumn, maxVal, incrementColumn);
        List<Map<String, Object>> rows = jdbc.queryForList(querySql);

        if (rows.isEmpty()) {
            log.info("无增量数据");
            return;
        }

        // 转换为 JSON 并通过 Doris Stream Load 写入
        String jsonData = JSONUtil.toJsonStr(rows);
        String result = streamLoadService.streamLoadJson(targetTable, jsonData);

        log.info("增量同步完成 - 源表: {}, 目标表: {}, 同步行数: {}, 批次: {}",
                sourceTable, targetTable, rows.size(), batch);
        log.info("增量同步完成 - 同步 {} 行数据到 {}", rows.size(), targetTable);
    }

    /**
     * 实时数据写入：直接将参数中的 JSON 数据写入 Doris 指定表
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/realtimeStreamLoad
     *   - Cron: 0 *\/1 * * * ?  （每分钟）
     *   - 参数: table_name
     */
    @PostMapping("/realtimeStreamLoad")
    public void realtimeStreamLoadHandler(@RequestParam(value = "tableName", required = false) String tableName) {
        if (tableName == null || tableName.isBlank()) {
            log.error("参数缺失：请传入目标表名");
            return;
        }

        log.info("实时数据写入 - 目标表: {}", tableName);

        // 从 Redis 或其他数据源获取待写入数据（此处简化示例）
        // 实际场景：从 Kafka 消费、从 API 拉取、从文件读取等
        // JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // List<Map<String, Object>> data = fetchDataFromSource();

        log.info("实时写入任务执行 - 目标表: {}", tableName);
    }

    /**
     * Doris 物化视图刷新：定期刷新 Doris 物化视图
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/refreshMaterializedView
     *   - Cron: 0 0 0/3 * * ?  （每3小时）
     *   - 参数: mv_name
     */
    @PostMapping("/refreshMaterializedView")
    public void refreshMaterializedViewHandler(@RequestParam(value = "mvName", required = false) String mvName) {
        if (mvName == null || mvName.isBlank()) {
            log.error("参数缺失：请传入物化视图名称");
            return;
        }

        log.info("刷新物化视图: {}", mvName);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        try {
            jdbc.execute("REFRESH MATERIALIZED VIEW " + mvName);
            log.info("物化视图 [{}] 刷新成功", mvName);
        } catch (Exception e) {
            log.error("刷新物化视图失败: {}", e.getMessage());
        }
    }
}
