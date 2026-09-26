package com.platform.service.integration.handler;

import cn.hutool.core.util.StrUtil;
import com.platform.service.integration.service.DataLakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 数据湖同步 Handler
 *
 * 支持以下数据同步模式：
 * 1. Hudi → Doris：通过 Multi-Catalog 联邦查询 + INSERT INTO SELECT
 * 2. Iceberg → Doris：同上
 * 3. Hive → Doris：同上
 * 4. MySQL → Doris：通过 JDBC Catalog 或 Stream Load
 * 5. Doris 物化视图定时刷新
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class DataLakeSyncHandler {

    private final DataLakeService dataLakeService;

    /**
     * 全量抽取：从数据湖表抽取到 Doris 内表
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/dataLakeExtract
     *   - Cron: 0 0 3 * * ?  （每天凌晨3点）
     *   - 参数: doris_table,source_catalog.source_db.source_table[,condition]
     *   示例: dws_orders,hudi_catalog.ods_db.hudi_orders,create_time>'2025-01-01'
     */
    @PostMapping("/dataLakeExtract")
    public void dataLakeExtractHandler(@RequestParam(value = "param", required = false) String param) {
        if (StrUtil.isBlank(param)) {
            log.error("参数缺失，格式: doris_table,source_catalog.source_db.source_table[,condition]");
            return;
        }

        String[] parts = param.split(",");
        if (parts.length < 2) {
            log.error("参数格式错误");
            return;
        }

        String dorisTable = parts[0];
        String sourceTable = parts[1];
        String condition = parts.length > 2 ? parts[2] : null;

        log.info("数据湖抽取 - 目标: {}, 源: {}, 条件: {}", dorisTable, sourceTable, condition);
        log.info("开始从 {} 抽取数据到 {}", sourceTable, dorisTable);

        int count = dataLakeService.extractFromDataLake(dorisTable, sourceTable, condition);
        log.info("抽取完成，影响行数: {}", count);
        log.info("数据湖抽取完成 - 影响 {} 行", count);
    }


     /** 增量同步：通过 Doris 物化视图自动刷新
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/refreshMv
     *   - Cron: 0 0 6 * * ?  （每6小时）
     *   - 参数: mv_name*/

    @PostMapping("/refreshMv")
    public void refreshMvHandler(@RequestParam(value = "mvName", required = false) String mvName) {
        if (StrUtil.isBlank(mvName)) {
            log.error("参数缺失：请传入物化视图名称");
            return;
        }

        log.info("刷新物化视图: {}", mvName);
        try {
            // 通过 Doris JDBC 执行 REFRESH
            dataLakeService.query("REFRESH MATERIALIZED VIEW " + mvName);
            log.info("物化视图 [{}] 刷新成功", mvName);
        } catch (Exception e) {
            log.error("刷新物化视图失败: {}", e.getMessage());
        }
    }

    /**
     * 跨源联邦查询：直接查询外部数据湖，返回结果
     *
     * DolphinScheduler 控制台配置：
     *   - 接口: POST /task/federatedQuery
     *   - 参数: SQL语句
     *   示例: SELECT COUNT(*) FROM hudi_catalog.ods_db.orders WHERE dt='2025-08-18'
     */
    @PostMapping("/federatedQuery")
    public void federatedQueryHandler(@RequestParam(value = "sql", required = false) String sql) {
        if (StrUtil.isBlank(sql)) {
            log.error("参数缺失：请传入SQL语句");
            return;
        }

        log.info("联邦查询: {}", sql);
        var result = dataLakeService.query(sql);
        log.info("查询结果行数: {}", result.size());
        result.forEach(row -> log.info("{}", row));
    }
}
