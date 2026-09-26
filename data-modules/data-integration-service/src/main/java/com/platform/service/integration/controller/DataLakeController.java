package com.platform.service.integration.controller;

import com.platform.common.core.R;
import com.platform.service.integration.service.DataLakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据湖集成")
@RestController
@RequestMapping("/integration")
public class DataLakeController {

    private final DataLakeService dataLakeService;

    public DataLakeController(DataLakeService dataLakeService) {
        this.dataLakeService = dataLakeService;
    }

    @Operation(summary = "创建 Hudi Catalog")
    @PostMapping("/catalog/hudi")
    public R<String> createHudiCatalog() {
        return R.ok(dataLakeService.createHudiCatalog());
    }

    @Operation(summary = "创建 Iceberg Catalog")
    @PostMapping("/catalog/iceberg")
    public R<String> createIcebergCatalog() {
        return R.ok(dataLakeService.createIcebergCatalog());
    }

    @Operation(summary = "创建 Hive Catalog")
    @PostMapping("/catalog/hive")
    public R<String> createHiveCatalog() {
        return R.ok(dataLakeService.createHiveCatalog());
    }

    @Operation(summary = "创建 MySQL Catalog")
    @PostMapping("/catalog/mysql")
    public R<String> createMysqlCatalog() {
        return R.ok(dataLakeService.createMysqlCatalog());
    }

    @Operation(summary = "创建 OceanBase Catalog")
    @PostMapping("/catalog/oceanbase")
    public R<String> createOceanBaseCatalog() {
        return R.ok(dataLakeService.createOceanBaseCatalog());
    }

    @Operation(summary = "创建 GBase 8s Catalog")
    @PostMapping("/catalog/gbase")
    public R<String> createGbaseCatalog() {
        return R.ok(dataLakeService.createGbaseCatalog());
    }

    @Operation(summary = "创建 达梦 DM Catalog")
    @PostMapping("/catalog/dameng")
    public R<String> createDamengCatalog() {
        return R.ok(dataLakeService.createDamengCatalog());
    }

    @Operation(summary = "创建 人大金仓 KingbaseES Catalog")
    @PostMapping("/catalog/kingbase")
    public R<String> createKingbaseCatalog() {
        return R.ok(dataLakeService.createKingbaseCatalog());
    }

    @Operation(summary = "联邦查询")
    @GetMapping("/query")
    public R<List<Map<String, Object>>> query(@RequestParam(value = "sql") String sql) {
        return R.ok(dataLakeService.query(sql));
    }

    @Operation(summary = "数据湖抽取到 Doris")
    @PostMapping("/extract")
    public R<Integer> extract(@RequestParam(value = "dorisTable") String dorisTable,
                              @RequestParam(value = "sourceTable") String sourceTable,
                              @RequestParam(value = "condition", required = false) String condition) {
        return R.ok(dataLakeService.extractFromDataLake(dorisTable, sourceTable, condition));
    }
}
