# OpenMetadata REST API 接口文档

## 概述

OpenMetadata 是一个开源的元数据管理平台，提供数据发现、血缘追踪、数据质量监控和治理能力。替代当前项目基于 Druid SQL Parser 的手动血缘解析逻辑。

- **默认地址**: `http://localhost:8585/api`
- **API 版本前缀**: `/v1/`
- **数据格式**: JSON（`Content-Type: application/json`）
- **Swagger UI**: 当前 `openmetadata/server:1.4.0` 镜像不提供该页面，请访问 `http://localhost:8585`，API 使用 `/api/v1/...`

### 组件依赖

| 组件 | 端口 | 说明 |
|------|------|------|
| OpenMetadata Server | 8585 | API + UI |
| MySQL/PostgreSQL | 3306/5432 | 元数据存储 |
| Elasticsearch | 9200 | 搜索引擎 |

## 与当前血缘方案的对比

| 对比项 | 当前方案（Druid SQL Parser） | OpenMetadata |
|--------|---------------------------|---------------|
| 血缘解析 | 手动解析 SQL AST | REST API 提交血缘边 |
| 元数据存储 | Doris 业务表 | 独立元数据库 |
| 搜索 | 无 | Elasticsearch 全文搜索 |
| 数据质量 | 自建规则引擎 | 内置测试框架 |
| 字段级血缘 | 支持（手动实现） | 支持（API 提交） |
| UI 可视化 | ECharts 自建 | 内置血缘图 |
| 标签/分类 | 无 | 完整标签体系 |

## 认证方式

所有 API 请求需在 Header 中携带 JWT Token：

```http
Authorization: Bearer {your_jwt_token}
Content-Type: application/json
```

### Token 类型

| 类型 | 用途 | 获取方式 |
|------|------|---------|
| Bot Token | 自动化/服务账号 | Settings → Bots → 生成 Token |
| Personal Access Token | 开发调试 | Profile → Access Tokens → 生成 |

> Bot Token 不过期，适合服务间集成。

---

## 1. 实体模型与 FQN 格式

OpenMetadata 使用**完全限定名称（FQN）**唯一标识实体：

```
service.database.schema.table
```

### 层级结构

```
DatabaseService (数据源连接)
  └── Database (数据库)
      └── DatabaseSchema (Schema)
          ├── Table (表)
          ├── Stored Procedure (存储过程)
          └── ...
```

### MySQL/Doris 的特殊映射

Doris 使用 MySQL 协议，映射为：

```
doris_service.default.doris_schema.table_name
         ↑          ↑           ↑          ↑
     服务名     默认数据库    Schema    表名
```

### EntityReference 结构

```json
{
  "id": "uuid-string",
  "type": "table",
  "name": "customers",
  "fullyQualifiedName": "doris_service.default.doris_schema.customers"
}
```

---

## 2. 数据库服务管理

**路径前缀**: `/v1/services/databaseServices`

替代当前项目中各服务 YAML 里的 Doris 数据源配置。

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/databaseServices` | 创建或更新服务 |
| POST | `/databaseServices` | 创建服务 |
| GET | `/databaseServices` | 列表查询 |
| GET | `/databaseServices/{id}` | 按 ID 查询 |
| GET | `/databaseServices/name/{fqn}` | 按名称查询 |
| PATCH | `/databaseServices/{id}` | 部分更新 |
| DELETE | `/databaseServices/{id}` | 删除 |
| PUT | `/databaseServices/restore` | 恢复软删除 |
| POST | `/databaseServices/{id}/testConnectionResult` | 测试连接 |

### 注册 Doris 数据源

```http
PUT /api/v1/services/databaseServices
```

```json
{
  "name": "doris_platform",
  "serviceType": "MySQL",
  "connection": {
    "config": {
      "type": "MySQL",
      "hostPort": "localhost:9030",
      "username": "root",
      "password": "",
      "databaseSchema": "data_platform"
    }
  },
  "description": "数据中台 Doris 数据源"
}
```

---

## 3. 表元数据管理

**路径前缀**: `/v1/tables`

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/tables` | 创建或更新表 |
| POST | `/tables` | 创建表 |
| GET | `/tables` | 列表查询 |
| GET | `/tables/{id}` | 按 ID 查询 |
| GET | `/tables/name/{fqn}` | 按 FQN 查询 |
| PATCH | `/tables/{id}` | 部分更新 |
| DELETE | `/tables/{id}` | 删除（软删除） |
| PUT | `/tables/restore` | 恢复软删除 |
| GET | `/tables/{id}/columns` | 查询字段 |
| PUT | `/tables/{id}/sampleData` | 添加样本数据 |
| GET | `/tables/{id}/tableProfile` | 查询表画像 |
| GET | `/tables/{id}/columnProfile` | 查询字段画像 |

### 创建表元数据

```http
PUT /api/v1/tables
```

```json
{
  "name": "dim_customer",
  "displayName": "客户维度表",
  "databaseSchema": "doris_platform.data_platform.doris_schema",
  "description": "客户主数据维度表",
  "tableType": "Regular",
  "columns": [
    {
      "name": "customer_id",
      "dataType": "BIGINT",
      "constraint": "PRIMARY_KEY",
      "description": "客户唯一标识"
    },
    {
      "name": "customer_name",
      "dataType": "VARCHAR",
      "dataLength": 256,
      "description": "客户名称"
    },
    {
      "name": "phone",
      "dataType": "VARCHAR",
      "dataLength": 20,
      "description": "手机号",
      "tags": [
        {"tagFQN": "PII.Sensitive"}
      ]
    },
    {
      "name": "created_at",
      "dataType": "TIMESTAMP",
      "description": "创建时间"
    }
  ]
}
```

### 查询参数

| 参数 | 说明 |
|------|------|
| `fields` | 展开字段：`owner,tags,followers,usageSummary,tableProfile,columns` |
| `include` | `all` / `non-deleted` / `deleted` |
| `limit` / `offset` | 分页 |
| `after` | 游标分页 |

---

## 4. 数据血缘

**路径前缀**: `/v1/lineage`

替代当前项目中 `data-lineage-service` 的 Druid SQL Parser 血缘解析。

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/lineage/{entityType}/{id}` | 查询血缘图 |
| PUT | `/lineage` | 添加血缘边 |
| DELETE | `/lineage/{entityType}/{id}` | 删除血缘边 |
| GET | `/lineage/{entityType}/{id}/export` | 导出血缘 CSV |

### 添加表级血缘

```http
PUT /api/v1/lineage
```

```json
{
  "edge": {
    "fromEntity": {
      "id": "source-table-uuid",
      "type": "table",
      "name": "ods_orders",
      "fullyQualifiedName": "doris_platform.data_platform.doris_schema.ods_orders"
    },
    "toEntity": {
      "id": "target-table-uuid",
      "type": "table",
      "name": "dwd_orders",
      "fullyQualifiedName": "doris_platform.data_platform.doris_schema.dwd_orders"
    },
    "description": "ODS → DWD 清洗转换",
    "lineageDetails": {
      "sqlQuery": "INSERT INTO dwd_orders SELECT order_id, customer_id FROM ods_orders",
      "pipeline": {
        "id": "pipeline-uuid",
        "type": "pipeline"
      }
    }
  }
}
```

### 添加字段级血缘

```json
{
  "edge": {
    "fromEntity": { "type": "table", "id": "src-uuid" },
    "toEntity": { "type": "table", "id": "tgt-uuid" },
    "lineageDetails": {
      "sqlQuery": "INSERT INTO target SELECT id, name FROM source",
      "columnsLineage": [
        {
          "fromColumns": [
            "doris_platform.data_platform.doris_schema.source.customer_id"
          ],
          "toColumn": "doris_platform.data_platform.doris_schema.target.cust_id"
        },
        {
          "fromColumns": [
            "doris_platform.data_platform.doris_schema.source.customer_name"
          ],
          "toColumn": "doris_platform.data_platform.doris_schema.target.name"
        }
      ]
    }
  }
}
```

### 查询血缘图

```http
GET /api/v1/lineage/table/{id}?upstreamDepth=3&downstreamDepth=3
```

```json
{
  "entity": {
    "id": "target-table-uuid",
    "type": "table",
    "name": "dwd_orders"
  },
  "nodes": [
    {"id": "src-uuid", "type": "table", "name": "ods_orders"},
    {"id": "tgt-uuid", "type": "table", "name": "dwd_orders"}
  ],
  "upstreamEdges": [
    {
      "fromEntity": "src-uuid",
      "toEntity": "tgt-uuid",
      "lineageDetails": {
        "sqlQuery": "INSERT INTO target SELECT id FROM source",
        "columnsLineage": [
          {"fromColumns": ["...source.id"], "toColumn": "...target.id"}
        ]
      }
    }
  ],
  "downstreamEdges": []
}
```

### 支持的实体类型

Table, Dashboard, Pipeline, Topic, ML Model, Container, Data Product

---

## 5. 搜索发现

**路径前缀**: `/v1/search`

### 搜索接口

```http
GET /api/v1/search/query?q=customer&index=table_search_index&from=0&size=10
```

| 参数 | 说明 |
|------|------|
| `q` | 搜索关键词，支持 Lucene 语法 |
| `index` | 搜索索引（`table_search_index` / `all`） |
| `from` / `size` | 分页 |
| `queryFilter` | Elasticsearch DSL 过滤 |
| `sortField` / `sortOrder` | 排序 |

### 搜索索引列表

| 索引 | 实体类型 |
|------|---------|
| `all` | 全部 |
| `table_search_index` | 表 |
| `pipeline_search_index` | 管道 |
| `dashboard_search_index` | 仪表盘 |
| `glossary_term_search_index` | 术语 |
| `tag_search_index` | 标签 |
| `user_search_index` | 用户 |

---

## 6. 数据质量

**路径前缀**: `/v1/dataQuality`

### 测试套件

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/dataQuality/testSuites` | 创建测试套件 |
| GET | `/dataQuality/testSuites` | 列表 |
| GET | `/dataQuality/testSuites/{id}` | 详情 |
| DELETE | `/dataQuality/testSuites/{id}` | 删除 |

### 测试用例

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/dataQuality/testCases` | 创建测试用例 |
| PUT | `/dataQuality/testCases` | 创建或更新 |
| GET | `/dataQuality/testCases` | 列表 |
| GET | `/dataQuality/testCases/{id}` | 详情 |
| GET | `/dataQuality/testCases/{id}/testCaseResult` | 查询结果 |
| PUT | `/dataQuality/testCases/{id}/testCaseResult` | 提交结果 |

### 创建测试用例

```http
POST /api/v1/dataQuality/testCases
```

```json
{
  "name": "row_count_check",
  "testDefinition": "tableRowCountToBeGreaterThan",
  "testSuite": "suite-uuid",
  "entityLink": "<#E::table::doris_platform.data_platform.doris_schema.dim_customer>",
  "parameterValues": [
    {"name": "minValue", "value": "1000"}
  ],
  "description": "客户表行数必须大于1000"
}
```

### 提交测试结果

```http
PUT /api/v1/dataQuality/testCases/{id}/testCaseResult
```

```json
{
  "result": "Success",
  "timestamp": 1704000000,
  "testResultValue": [
    {"name": "rowCount", "value": "5000"}
  ]
}
```

### 内置测试定义

| 测试定义 | 说明 |
|---------|------|
| `tableRowCountToBeGreaterThan` | 行数大于阈值 |
| `tableRowCountToEqual` | 行数等于值 |
| `columnValueToBeUnique` | 字段值唯一 |
| `columnValueToNotBeNull` | 字段值非空 |
| `columnValueMaxToBeBetween` | 最大值在范围内 |
| `columnValueMinToBeBetween` | 最小值在范围内 |
| `columnValuesToBeNotInSet` | 值不在集合中 |
| `columnValuesToMatchRegex` | 值匹配正则 |

---

## 7. Pipeline 元数据

**路径前缀**: `/v1/pipelines`

注册 DolphinScheduler 的工作流为 OpenMetadata 中的 Pipeline 实体。

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/pipelines` | 创建或更新 |
| GET | `/pipelines` | 列表 |
| GET | `/pipelines/{id}` | 详情 |
| GET | `/pipelines/name/{fqn}` | 按名称查询 |
| PATCH | `/pipelines/{id}` | 更新 |
| DELETE | `/pipelines/{id}` | 删除 |

### 注册 Pipeline

```http
PUT /api/v1/pipelines
```

```json
{
  "name": "etl_daily_workflow",
  "displayName": "每日ETL工作流",
  "pipelineService": "dolphinscheduler_service",
  "fullyQualifiedName": "dolphinscheduler_service.etl_daily_workflow",
  "description": "数据中台每日ETL调度工作流",
  "pipelineUrl": "http://localhost:12345/dolphinscheduler/ui/projects/1/workflow/definitions/100",
  "tasks": [
    {"name": "extract-data", "taskUrl": "..."},
    {"name": "transform-data", "taskUrl": "..."},
    {"name": "load-data", "taskUrl": "..."}
  ]
}
```

---

## 8. 标签与分类

### 分类

**路径前缀**: `/v1/classifications`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/classifications` | 创建或更新 |
| GET | `/classifications` | 列表 |
| DELETE | `/classifications/{id}` | 删除 |

### 标签

**路径前缀**: `/v1/tags`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/tags` | 创建或更新 |
| GET | `/tags` | 列表 |
| DELETE | `/tags/{id}` | 删除 |

### 给表打标签

```http
PUT /api/v1/tables/{id}/tags
```

```json
{
  "tags": [
    {"tagFQN": "PII.Sensitive"},
    {"tagFQN": "Certification.Gold"}
  ]
}
```

---

## 9. Glossary（业务术语表）

**路径前缀**: `/v1/glossaries` + `/v1/glossaryTerms`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/glossaries` | 创建术语库 |
| GET | `/glossaries` | 列表 |
| PUT | `/glossaryTerms` | 创建术语 |
| GET | `/glossaryTerms` | 列表 |
| PATCH | `/glossaryTerms/{id}` | 更新术语 |
| DELETE | `/glossaryTerms/{id}` | 删除术语 |

### FQN 格式

```
GlossaryName.TermName
GlossaryName.ParentTerm.ChildTerm
```

---

## 10. Webhook 事件订阅

```http
POST /api/v1/webhook
```

```json
{
  "name": "lineage-change-webhook",
  "endpoint": "http://data-platform:8805/lineage/webhook",
  "eventFilters": ["entityCreated", "entityUpdated", "entitySoftDeleted"],
  "batchSize": 50,
  "enabled": true,
  "secretKey": "your-secret-key"
}
```

OpenMetadata 发送事件时携带 `X-OM-Signature` HMAC 签名头，用于验证请求来源。

---

## 错误响应

```json
{
  "code": 400,
  "message": "Validation error",
  "details": "Field 'name' is required"
}
```

| Code | 说明 |
|------|------|
| 400 | 请求参数错误 |
| 401 | 未认证或 Token 无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 实体已存在（FQN 冲突） |
| 429 | 限流 |

---

## 分页格式

### 列表分页

```json
{
  "data": [],
  "paging": {
    "after": "cursor-string",
    "total": 100
  }
}
```

### 搜索分页

```json
{
  "took": 8,
  "timed_out": false,
  "hits": {
    "total": {"value": 53, "relation": "eq"},
    "hits": []
  }
}
```

---

## 与数据中台集成流程

```
1. 注册数据源           PUT /v1/services/databaseServices (Doris)
2. 注册表元数据         PUT /v1/tables (自动从 Doris 拉取 schema)
3. 注册 Pipeline        PUT /v1/pipelines (DolphinScheduler 工作流)
4. 提交血缘关系         PUT /v1/lineage (ETL 完成后自动提交)
5. 配置数据质量测试     POST /v1/dataQuality/testCases
6. 订阅变更事件         POST /v1/webhook
7. 前端搜索元数据       GET /v1/search/query
```

