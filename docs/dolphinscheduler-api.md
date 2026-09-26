# Apache DolphinScheduler REST API 接口文档

## 概述

DolphinScheduler 是一个分布式、易扩展的可视化 DAG 工作流调度系统，支持拖拽方式配置任务依赖关系，适合大数据平台复杂调度场景。

- **默认地址**: `http://{host}:12345/dolphinscheduler/api`
- **Swagger UI**: `http://{host}:12345/dolphinscheduler/swagger-ui/index.html`
- **数据格式**: JSON（`Content-Type: application/json`）
- **统一响应格式**:

```json
{
  "code": 0,       // 0=成功，非0=失败
  "msg": "success",
  "data": {}       // 业务数据，无返回值时为 null
}
```

## 与 XXL-Job 的对比

| 对比项 | XXL-Job | DolphinScheduler |
|--------|---------|-------------------|
| 任务配置 | 代码写死 Handler + Cron | 拖拽 DAG + Cron |
| 依赖关系 | 无（单任务独立调度） | 有（任务间上下游依赖） |
| 任务类型 | BEAN 模式（Java Handler） | SHELL/SQL/Python/Spark/Flink 等 30+ 内置 |
| 调度粒度 | 单任务 | 工作流（DAG） |
| 补数能力 | 无 | 支持指定时间范围补数 |
| 实时任务 | 支持 | 支持（Stream Task） |
| 鉴权方式 | Cookie/Session（模拟登录） | Token（Security Center 生成） |

## 认证方式

DolphinScheduler 使用 **Access Token** 认证，比 XXL-Job 的 Cookie/Session 更规范。

### 获取 Token

1. 登录 DolphinScheduler Web UI
2. 安全中心 → Token 管理 → 新建 Token
3. 指定用户和过期时间
4. 生成后复制 Token 字符串

### API 调用携带 Token

```http
token: {your_generated_token}
Content-Type: application/json
```

### Token 管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/access-tokens` | 创建 Token |
| POST | `/access-tokens/generate` | 生成 Token 字符串 |
| GET | `/access-tokens` | 分页查询 Token 列表 |
| GET | `/access-tokens/user/{userId}` | 查询指定用户的 Token |
| PUT | `/access-tokens/{id}` | 更新 Token |
| DELETE | `/access-tokens/{id}` | 删除 Token |

---

## 1. 项目管理

**路径前缀**: `/projects`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/projects` | 创建项目 |
| PUT | `/projects/{code}` | 更新项目 |
| GET | `/projects/{code}` | 查询项目详情 |
| GET | `/projects` | 分页查询项目列表 |
| GET | `/projects/list` | 查询全部项目 |
| DELETE | `/projects/{code}` | 删除项目 |

### 创建项目

```http
POST /dolphinscheduler/api/projects
```

```json
{
  "projectName": "data-platform",
  "description": "数据中台调度项目"
}
```

---

## 2. 工作流定义（DAG）

**路径前缀**: `/projects/{projectCode}/workflow-definitions`

工作流是 DolphinScheduler 的核心概念——一个 DAG，包含多个任务节点和依赖关系。

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/workflow-definitions` | 创建工作流 |
| PUT | `/workflow-definitions/{code}` | 更新工作流 |
| GET | `/workflow-definitions` | 查询工作流列表 |
| GET | `/workflow-definitions/{code}` | 查询工作流详情 |
| GET | `/workflow-definitions/{code}/genDagGraph` | 生成 DAG 图结构 |
| GET | `/workflow-definitions/{code}/genDagData` | 获取工作流数据模型 |
| GET | `/workflow-definitions/{code}/versions` | 查询历史版本 |
| DELETE | `/workflow-definitions/{code}` | 删除工作流 |

### 创建工作流

```http
POST /dolphinscheduler/api/projects/{projectCode}/workflow-definitions
```

```json
{
  "name": "etl-daily-workflow",
  "description": "每日ETL工作流",
  "taskDefinitionJsonObj": {
    "tasks": [
      {
        "code": 100001,
        "name": "extract-data",
        "type": "SQL",
        "datasource": 1,
        "sql": "SELECT * FROM source_table WHERE dt='${dt}'"
      },
      {
        "code": 100002,
        "name": "transform-data",
        "type": "SHELL",
        "script": "python /opt/scripts/transform.py --date ${dt}"
      },
      {
        "code": 100003,
        "name": "load-data",
        "type": "SQL",
        "datasource": 2,
        "sql": "INSERT INTO target_table SELECT * FROM temp_table"
      }
    ],
    "relations": [
      {"preTaskCode": 100001, "postTaskCode": 100002},
      {"preTaskCode": 100002, "postTaskCode": 100003}
    ]
  }
}
```

### DAG 存储结构

```
工作流定义 (WorkflowDefinition)
├── 元数据: name, description, code, version
├── 任务定义列表 (TaskDefinition)
│   ├── extract-data (SQL) ← code: 100001
│   ├── transform-data (SHELL) ← code: 100002
│   └── load-data (SQL) ← code: 100003
└── 依赖关系 (TaskRelation)
    ├── 100001 → 100002
    └── 100002 → 100003
```

---

## 3. 任务定义

**路径前缀**: `/projects/{projectCode}/task-definition`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/task-definition/{code}/with-upstream` | 更新任务定义（带上游依赖） |
| GET | `/task-definition/{code}` | 查询任务定义详情 |
| GET | `/task-definition/gen-task-codes?genNum=5` | 生成任务码 |
| GET | `/task-definition/{code}/versions` | 查询任务历史版本 |
| POST | `/task-definition/{code}/release?releaseState=ONLINE` | 上线/下线任务 |

### 生成任务码

```http
GET /dolphinscheduler/api/projects/{projectCode}/task-definition/gen-task-codes?genNum=3
```

```json
{
  "code": 0,
  "msg": "success",
  "data": [100001, 100002, 100003]
}
```

### 内置任务类型

| 类型 | 说明 | 适用场景 |
|------|------|---------|
| SHELL | Shell 脚本 | 通用脚本执行 |
| SQL | SQL 语句 | 数据抽取/加载 |
| PYTHON | Python 脚本 | 数据处理 |
| SPARK | Spark 任务 | 大规模数据处理 |
| FLINK | Flink 任务 | 实时计算 |
| HTTP | HTTP 请求 | API 调用 |
| SUB_PROCESS | 子工作流 | 工作流嵌套 |
| DEPENDENT | 跨工作流依赖 | 上游工作流依赖 |
| DATA_QUALITY | 数据质量检查 | 质量校验 |
| SWITCH | 条件分支 | DAG 条件路由 |
| CONDITIONS | 条件判断 | 成功/失败分支 |

---

## 4. 调度管理

**路径前缀**: `/projects/{projectCode}/schedules`

替代 XXL-Job 的 Cron 配置功能。

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/schedules` | 创建调度 |
| PUT | `/schedules/{id}` | 更新调度 |
| GET | `/schedules` | 查询调度列表 |
| DELETE | `/schedules/{id}` | 删除调度 |
| POST | `/schedules/{id}/online` | 上线调度（开始自动执行） |
| POST | `/schedules/{id}/offline` | 下线调度（停止自动执行） |
| POST | `/schedules/preview` | 预览调度时间 |

### 创建调度

```http
POST /dolphinscheduler/api/projects/{projectCode}/schedules
```

```json
{
  "workflowDefinitionCode": 100,
  "schedule": {
    "startTime": "2026-08-20 00:00:00",
    "endTime": "2099-12-31 00:00:00",
    "timezoneId": "Asia/Shanghai",
    "crontab": "0 0 2 * * ? *"
  },
  "warningType": "ALL",
  "warningGroupId": 1,
  "failureStrategy": "CONTINUE",
  "workerGroup": "default",
  "tenantCode": "default",
  "environmentCode": -1,
  "workflowInstancePriority": "MEDIUM"
}
```

### 上线/下线

```http
POST /dolphinscheduler/api/projects/{projectCode}/schedules/{id}/online
POST /dolphinscheduler/api/projects/{projectCode}/schedules/{id}/offline
```

---

## 5. 工作流执行

**路径前缀**: `/projects/{projectCode}/executors`

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/executors/start-workflow-instance` | 启动工作流 |
| POST | `/executors/batch-start-workflow-instance` | 批量启动 |
| POST | `/executors/execute` | 操作工作流实例 |
| POST | `/executors/batch-execute` | 批量操作 |
| POST | `/executors/backfill` | 补数 |
| POST | `/executors/task-instance/{code}/start` | 启动实时任务 |

### 启动工作流

```http
POST /dolphinscheduler/api/projects/{projectCode}/executors/start-workflow-instance
```

```json
{
  "workflowDefinitionCode": 100,
  "scheduleTime": "2026-08-20 00:00:00",
  "failureStrategy": "END",
  "startNodeList": "",
  "taskDependType": "NONE",
  "execType": "EXECUTE",
  "warningType": "ALL",
  "warningGroupId": 1,
  "runMode": "NORMAL",
  "workflowInstancePriority": "MEDIUM",
  "workerGroup": "default",
  "tenantCode": "default",
  "environmentCode": -1,
  "startParams": {},
  "dryRun": 0
}
```

### 操作工作流实例

```http
POST /dolphinscheduler/api/projects/{projectCode}/executors/execute
```

```json
{
  "workflowInstanceId": 1001,
  "executeType": "STOP"
}
```

| executeType | 说明 |
|-------------|------|
| STOP | 强制停止 |
| REPEAT_RUNNING | 从头重新运行 |
| START_FAILURE_TASK_PROCESS | 重试失败任务 |
| RECOVER_SUSPENDED_PROCESS | 恢复暂停的工作流 |

### 补数（数据回溯）

```http
POST /dolphinscheduler/api/projects/{projectCode}/executors/backfill
```

```json
{
  "workflowDefinitionCode": 100,
  "scheduleTime": "2026-08-10 00:00:00,2026-08-20 00:00:00",
  "failureStrategy": "CONTINUE",
  "warningType": "ALL",
  "runMode": "CONCURRENT",
  "workerGroup": "default",
  "tenantCode": "default",
  "dryRun": 0
}
```

---

## 6. 数据源管理

**路径前缀**: `/datasources`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/datasources` | 创建数据源 |
| PUT | `/datasources/{id}` | 更新数据源 |
| GET | `/datasources/{id}` | 查询数据源详情 |
| GET | `/datasources` | 分页查询 |
| GET | `/datasources/list` | 按类型查询 |
| POST | `/datasources/connect` | 测试新连接 |
| GET | `/datasources/{id}/connect-test` | 测试已有连接 |
| GET | `/datasources/tables` | 查询表列表 |
| GET | `/datasources/tableColumns` | 查询表字段 |
| DELETE | `/datasources/{id}` | 删除数据源 |

### 创建数据源

```http
POST /dolphinscheduler/api/datasources
```

```json
{
  "type": "MYSQL",
  "name": "doris-source",
  "host": "localhost",
  "port": 9030,
  "userName": "root",
  "password": "",
  "database": "data_platform",
  "other": {
    "serverTimezone": "GMT-8"
  }
}
```

### 支持的数据源类型

| 类型 | 说明 |
|------|------|
| MYSQL | MySQL / Doris（MySQL 协议） |
| POSTGRESQL | PostgreSQL |
| HIVE | Hive |
| SPARK | Spark ThriftServer |
| CLICKHOUSE | ClickHouse |
| ORACLE | Oracle |
| SQLSERVER | SQL Server |
| DB2 | DB2 |
| PRESTO | Presto |

---

## 7. 资源管理

**路径前缀**: `/resources`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/resources` | 上传文件 |
| POST | `/resources/directory` | 创建目录 |
| POST | `/resources/online-create` | 在线创建文件 |
| PUT | `/resources/update-content` | 更新文件内容 |
| GET | `/resources` | 分页查询 |
| GET | `/resources/list` | 按类型查询 |
| GET | `/resources/view` | 查看文件内容 |
| GET | `/resources/download` | 下载文件 |
| DELETE | `/resources` | 删除资源 |

---

## 8. Worker Group 管理

**路径前缀**: `/worker-groups`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/worker-groups` | 创建/更新 Worker Group |
| GET | `/worker-groups` | 分页查询 |
| GET | `/worker-groups/all` | 查询全部 |
| GET | `/worker-groups/worker-address-list` | 查询 Worker 地址列表 |
| DELETE | `/worker-groups/{id}` | 删除 Worker Group |

---

## 通过 API 编程式创建 DAG 完整流程

```
1. 创建项目                    POST /projects
2. 生成任务码                  GET  /projects/{code}/task-definition/gen-task-codes?genNum=3
3. 创建任务定义（带依赖关系）    PUT  /projects/{code}/task-definition/{code}/with-upstream
4. 创建工作流定义（组装 DAG）   POST /projects/{code}/workflow-definitions
5. 创建调度配置                POST /projects/{code}/schedules
6. 上线调度                    POST /projects/{code}/schedules/{id}/online
7. 手动触发执行                POST /projects/{code}/executors/start-workflow-instance
```

> **与 XXL-Job 的关键区别**：DolphinScheduler 的任务是 DAG（有向无环图），任务之间有依赖关系；XXL-Job 每个任务独立调度，无依赖。DolphinScheduler 通过 `relations` 数组定义上下游依赖。
