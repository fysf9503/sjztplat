# 任务依赖编排设计方案

> 基于 DolphinScheduler + 数据血缘的自动化任务依赖管理

---

## 一、设计思路

### 用户原始方案

1. 在数据开发界面创建开发任务（Flink、SQL 等）
2. 存到数据库
3. 打开 DolphinScheduler Web UI
4. 在 DS 里拖一个 Flink 节点，搜索任务名，配置任务
5. 在 DS 里手动设置任务依赖关系（Flink 依赖集成任务）

### 改进方案

**核心改进：不需要去 DS 里拖拽配置，在我们的平台里统一管理，一键发布。**

| 步骤 | 用户方案 | 改进方案 |
|------|---------|---------|
| 创建任务 | 平台创建 | 平台创建 → **自动注册到 DS** |
| 配置依赖 | 手动在 DS 拖拽 | **根据表血缘自动推导** |
| 发布工作流 | 手动在 DS 配置 | **一键发布，自动生成 DAG 推送到 DS** |
| 调度管理 | DS 里管理 | 平台统一管理，DS 作为执行引擎 |

---

## 二、核心架构

```
┌────────────────────────────────────────────────────────────┐
│                    我们的前端 (数据开发界面)                    │
│  ┌──────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │ 任务管理   │  │ 工作流编排    │  │ DAG 可视化 (ECharts) │  │
│  │ /dev/task │  │ /dev/workflow│  │ 节点+边+自动推导     │  │
│  └─────┬─────┘  └──────┬───────┘  └──────────┬──────────┘  │
└────────┼───────────────┼─────────────────────┼─────────────┘
         │               │                     │
         ▼               ▼                     ▼
┌────────────────────────────────────────────────────────────┐
│                  system-service (后端 API)                  │
│                                                             │
│  DevTaskService          DevWorkflowService                 │
│  ├── 创建任务             ├── 创建工作流                      │
│  ├── 自动注册到DS         ├── 添加任务到工作流                │
│  ├── 管理源/目标表        ├── 自动检测依赖 (表血缘)           │
│  └── 同步更新             ├── 构建 DAG JSON                  │
│                          ├── 一键发布到DS                    │
│                          └── 上线/下线调度                   │
│                                                             │
│  data-common-ds (DS REST Client)                           │
│  ├── createTaskDefinition()  → DS API: /task-definitions    │
│  ├── createWorkflowWithDag() → DS API: /workflow-definitions│
│  └── scheduleClient         → DS API: /schedules           │
└─────────────────────────┬──────────────────────────────────┘
                          │ REST API
                          ▼
┌────────────────────────────────────────────────────────────┐
│              DolphinScheduler (执行引擎)                     │
│  ├── Task Definitions (任务定义仓库)                        │
│  ├── Workflow Definitions (工作流 DAG)                      │
│  ├── Schedules (调度管理)                                   │
│  └── Executors (任务执行)                                   │
└────────────────────────────────────────────────────────────┘
```

---

## 三、数据库表设计

### 3.1 dev_task（开发任务定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) | 主键 |
| task_name | VARCHAR(128) | 任务名称 |
| task_type | VARCHAR(32) | FLINK / SQL / SHELL / PYTHON / CUSTOM |
| task_params | TEXT | DS taskParams JSON |
| source_tables | TEXT | 源表 FQN JSON 数组（任务读取的表） |
| target_tables | TEXT | 目标表 FQN JSON 数组（任务写入的表） |
| ds_task_code | BIGINT | DS 任务定义 Code |
| status | VARCHAR(16) | DRAFT / PUBLISHED |

### 3.2 dev_workflow（开发工作流）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) | 主键 |
| workflow_name | VARCHAR(128) | 工作流名称 |
| cron_expr | VARCHAR(128) | Cron 调度表达式 |
| ds_workflow_code | BIGINT | DS 工作流 Code |
| ds_schedule_id | BIGINT | DS 调度 ID |
| status | VARCHAR(16) | DRAFT / PUBLISHED / ONLINE / OFFLINE |

### 3.3 dev_workflow_task（工作流-任务关联+依赖）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) | 主键 |
| workflow_id | VARCHAR(32) | 工作流 ID |
| task_id | VARCHAR(32) | 任务 ID |
| ds_task_code | BIGINT | DS 任务 Code |
| upstream_task_ids | TEXT | 上游任务 ID JSON 数组 |
| dependency_type | VARCHAR(16) | AUTO(自动推导待确认)/MANUAL(手动添加)/CONFIRMED(已确认) |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 四、自动依赖检测算法

### 核心规则

```
如果任务 A 的 sourceTables ∩ 任务 B 的 targetTables ≠ ∅
则 A 依赖 B（B 是 A 的上游，B 执行完后才能执行 A）
```

### 示例场景

```
集成任务 T1: source=[外部数据源], target=[ods.user_log]
集成任务 T2: source=[外部数据源], target=[ods.order_data]
Flink 任务 T3: source=[ods.user_log, ods.order_data], target=[dws.user_analytics]

自动推导结果:
  T3 的 sourceTables = [ods.user_log, ods.order_data]
  T1 的 targetTables = [ods.user_log]     → 交集! T3 依赖 T1
  T2 的 targetTables = [ods.order_data]   → 交集! T3 依赖 T2

DAG: T1 ──┐
          ├──→ T3
    T2 ──┘
```

### 算法伪代码

```
for each task A in workflow:
    for each task B in workflow (B != A):
        if A.sourceTables ∩ B.targetTables ≠ ∅:
            add dependency: A depends on B
```

---

## 五、API 接口

### 5.1 任务管理 `/dev/task`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dev/task/list` | 任务列表（支持按类型/状态筛选） |
| GET | `/dev/task/{id}` | 任务详情 |
| POST | `/dev/task/create` | 创建任务（自动注册到 DS） |
| PUT | `/dev/task/{id}` | 更新任务（同步到 DS） |
| DELETE | `/dev/task/{id}` | 删除任务（同步删除 DS 定义） |
| GET | `/dev/task/{id}/ds-detail` | 查看 DS 端任务详情 |

### 5.2 工作流管理 `/dev/workflow`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dev/workflow/list` | 工作流列表 |
| GET | `/dev/workflow/{id}` | 工作流详情 |
| POST | `/dev/workflow/create` | 创建工作流 |
| DELETE | `/dev/workflow/{id}` | 删除工作流 |
| POST | `/dev/workflow/{id}/task` | 添加任务到工作流 |
| DELETE | `/dev/workflow/{wid}/task/{tid}` | 移除任务 |
| GET | `/dev/workflow/{id}/tasks` | 工作流中的任务列表 |
| GET | `/dev/workflow/{id}/dag` | DAG 可视化数据 |
| GET | `/dev/workflow/{id}/dependency-suggestions` | 获取自动检测的依赖建议（不修改数据库） |
| POST | `/dev/workflow/{id}/confirm-dependencies` | 确认依赖关系（用户确认后保存） |
| POST | `/dev/workflow/{id}/manual-dependency` | 手动添加前置依赖 |
| DELETE | `/dev/workflow/{id}/dependency` | 移除一条前置依赖 |
| GET | `/dev/workflow/{id}/search-tasks` | 搜索可添加为前置依赖的任务 |
| POST | `/dev/workflow/{id}/publish` | 发布到 DS（要求依赖已确认） |
| POST | `/dev/workflow/{id}/online` | 上线调度 |
| POST | `/dev/workflow/{id}/offline` | 下线调度 |

---

## 六、使用流程（完整示例）

### 步骤 1: 创建集成任务

```bash
POST /dev/task/create
{
    "taskName": "同步用户日志",
    "taskType": "SQL",
    "taskParams": "{\"dataSource\":1,\"sql\":\"INSERT INTO ods.user_log SELECT * FROM external.user_log\"}",
    "sourceTables": "[\"external.user_log\"]",
    "targetTables": "[\"ods.user_log\"]",
    "description": "从外部数据源同步用户日志到ODS层"
}
```

### 步骤 2: 创建 Flink 任务

```bash
POST /dev/task/create
{
    "taskName": "用户行为分析",
    "taskType": "FLINK",
    "taskParams": "{\"flinkJar\":\"hdfs:///jobs/UserAnalytics.jar\",\"mainClass\":\"com.platform.UserAnalytics\",\"parallelism\":4}",
    "sourceTables": "[\"ods.user_log\",\"ods.order_data\"]",
    "targetTables": "[\"dws.user_analytics\"]",
    "description": "Flink实时分析用户行为，结果写入DWS层"
}
```

### 步骤 3: 创建工作流并添加任务

```bash
# 创建工作流
POST /dev/workflow/create
{
    "workflowName": "用户数据分析流程",
    "description": "同步+计算完整流程",
    "cronExpr": "0 0 2 * * ?"
}

# 添加集成任务（upstreamTaskIds 为空 → 自动推导）
POST /dev/workflow/{workflowId}/task
{
    "taskId": "<集成任务ID>",
    "upstreamTaskIds": null
}

# 添加 Flink 任务（upstreamTaskIds 为空 → 自动推导）
POST /dev/workflow/{workflowId}/task
{
    "taskId": "<Flink任务ID>",
    "upstreamTaskIds": null
}
```

### 步骤 4: 自动检测依赖建议

```bash
GET /dev/workflow/{workflowId}/dependency-suggestions
```

系统基于表血缘自动推导依赖建议，**仅返回建议不修改数据库**。
返回示例：Flink 任务读取 `ods.user_log`，集成任务写入 `ods.user_log`，建议 Flink 依赖集成任务。

### 步骤 5: 确认依赖关系

前端展示自动检测的建议，用户可勾选确认，也可手动搜索任务添加前置依赖：

```bash
# 确认自动检测的依赖
POST /dev/workflow/{workflowId}/confirm-dependencies
{
    "flinkTaskId": ["integrationTaskId1", "integrationTaskId2"],
    "integrationTaskId1": [],
    "integrationTaskId2": []
}

# 或手动添加前置依赖
POST /dev/workflow/{workflowId}/manual-dependency
{
    "taskId": "flinkTaskId",
    "upstreamTaskId": "integrationTaskId1"
}

# 搜索可依赖的任务
GET /dev/workflow/{workflowId}/search-tasks?keyword=同步
```

### 步骤 6: 查看 DAG

```bash
GET /dev/workflow/{workflowId}/dag
```

返回 ECharts 可渲染的 nodes + edges 数据，前端展示 DAG 图。

### 步骤 7: 发布到 DS

```bash
POST /dev/workflow/{workflowId}/publish
```

**发布前校验：所有任务的依赖状态必须为 CONFIRMED 或 MANUAL，AUTO 状态的任务会被拒绝。**
系统构建 DS `taskDefinitionJsonObj` + `taskRelationJson`，调用 DS API 创建完整 DAG 工作流。

### 步骤 8: 上线

```bash
POST /dev/workflow/{workflowId}/online
```

系统创建 DS 调度（Cron 表达式）并上线，DS 按 Cron 自动执行 DAG。

---

## 七、DS 任务关系 JSON 格式

发布时自动生成的 `taskRelationJson`（DAG 边）:

```json
[
    {
        "preTaskCode": 0,
        "postTaskCode": 12345678901234,
        "conditionType": "NONE",
        "conditionParams": {}
    },
    {
        "preTaskCode": 12345678901234,
        "postTaskCode": 56789012345678,
        "conditionType": "NONE",
        "conditionParams": {}
    }
]
```

- `preTaskCode=0`：起始节点（无上游依赖的任务）
- `preTaskCode=X, postTaskCode=Y`：X 执行完后执行 Y

---

## 八、各任务类型 taskParams 模板

### FLINK 任务

```json
{
    "flinkJar": "hdfs:///jobs/UserAnalytics.jar",
    "mainClass": "com.platform.UserAnalytics",
    "mainArgs": "--env prod --parallelism 4",
    "parallelism": 4,
    "deployMode": "cluster",
    "flinkVersion": "1.18"
}
```

### SQL 任务

```json
{
    "type": "MYSQL",
    "datasource": 1,
    "sql": "INSERT INTO ods.user_log SELECT * FROM external.user_log",
    "sendEmail": false
}
```

### SHELL 任务（调用我们平台的 REST 接口）

```json
{
    "shellContent": "curl -X POST http://system-service:8800/task/qualityCheck",
    "runFlag": "NORMAL"
}
```

### PYTHON 任务

```json
{
    "pythonPath": "",
    "rawScript": "import requests\nr = requests.post('http://system-service:8800/task/dataLakeExtract')",
    "resourceList": []
}
```
