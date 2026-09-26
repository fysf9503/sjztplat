# 架构升级说明：DolphinScheduler + OpenMetadata 集成方案

## 一、升级目标

| 当前方案 | 升级目标 | 替换原因 |
|---------|---------|---------|
| XXL-Job | DolphinScheduler | 支持拖拽 DAG、任务依赖、补数能力，更契合大数据平台复杂调度 |
| Druid SQL Parser 血缘 | OpenMetadata | 专业元数据平台，内置血缘图、搜索、数据质量、标签体系 |
| data-lineage-service 手动解析 | OpenMetadata API | 减少自研维护成本，用标准平台替代 |

## 二、整体架构

```
                        ┌───────────────────────────────────────────┐
                        │              前端 (Vue 3)                   │
                        │  /schedule/*  /lineage/*  /metadata/*    │
                        └───────────────────┬───────────────────────┘
                                            │
                        ┌───────────────────▼───────────────────────┐
                        │           data-gateway (8601)             │
                        │           JWT 统一鉴权                    │
                        └───┬───────┬───────┬───────┬───────┬───────┘
                            │       │       │       │       │
              ┌─────────────▼┐ ┌───▼────┐ ┌▼────────┐ ┌▼──────┐ ┌▼──────────────┐
              │system-service │ │quality│ │realtime │ │integra│ │data-lineage   │
              │   (8800)      │ │(8801) │ │(8802)   │ │tion   │ │  -service     │
              │               │ │       │ │         │ │(8803) │ │  (8805)       │
              │ 调度配置管理   │ │数据质量│ │实时任务 │ │数据集成│ │血缘→OM代理    │
              └──────┬────────┘ └───┬───┘ └────┬───┘ └──┬────┘ └──────┬───────┘
                     │              │          │        │              │
         ┌───────────▼──────────────▼──────────▼────────▼──────┐      │
         │              Nacos 服务发现 + Feign 调用              │      │
         └──────────────────────────────────────────────────────┘      │
                            │           │                             │
              ┌─────────────▼──┐  ┌─────▼──────────┐  ┌─────────────▼──┐
              │ DolphinScheduler│  │  Apache Doris  │  │ OpenMetadata  │
              │   (12345)      │  │  (9030/8030)   │  │   (8585)      │
              │                │  │                 │  │               │
              │ DAG 调度引擎    │  │ 数据仓库        │  │ 元数据+血缘   │
              │ 拖拽配置工作流  │  │                 │  │ 数据质量+搜索 │
              └────────────────┘  └─────────────────┘  └───────────────┘
```

## 三、模块独立性原则

### 核心原则：每个模块通过 REST API 调用外部系统，无源码级依赖

```
❌ 错误做法：import org.apache.dolphinscheduler.api.controller.*
✅ 正确做法：RestTemplate → POST http://dolphinscheduler:12345/dolphinscheduler/api/...
```

### 依赖关系矩阵

| 模块 | DolphinScheduler | OpenMetadata | Doris | 依赖方式 |
|------|:---:|:---:|:---:|------|
| system-service | ✅ 调度管理 | - | ✅ 业务数据 | RestTemplate / JPA |
| data-quality-service | ✅ 质量任务调度 | ✅ 测试结果同步 | ✅ 质量规则存储 | RestTemplate / JPA |
| data-realtime-service | ✅ 实时任务调度 | - | ✅ Stream Load | RestTemplate / JPA |
| data-integration-service | ✅ ETL 调度 | ✅ 血缘提交 | ✅ 数据集成 | RestTemplate / JPA |
| data-lineage-service | - | ✅ 血缘代理 | - | RestTemplate |
| data-governance-service | ✅ 治理任务调度 | ✅ 元数据同步 | ✅ 治理数据 | RestTemplate / JPA |

> ✅ = 通过 HTTP 调用，不是 Maven 依赖。外部系统宕机不影响本模块核心功能（降级处理）。

### 降级策略

```java
// 每个外部调用都需要降级处理
public class DolphinSchedulerClient {

    public WorkflowInstance startWorkflow(Long workflowCode) {
        try {
            return restTemplate.postForObject(
                dsAddress + "/projects/{code}/executors/start-workflow-instance",
                request, WorkflowInstance.class, projectCode);
        } catch (Exception e) {
            log.warn("DolphinScheduler 不可用，任务已记录到本地队列等待重试: {}", e.getMessage());
            saveToRetryQueue(workflowCode);
            return null;
        }
    }
}
```

## 四、DolphinScheduler 替换 XXL-Job

### 架构变化

```
替换前 (XXL-Job):
┌──────────┐     Cookie/Session     ┌──────────────┐
│ system-  │ ─────────────────────→ │ XXL-Job Admin│
│ service  │   /jobinfo/insert       │   (8888)     │
│ JobConfig│   /jobinfo/start        │              │
│ Service   │   ids[]=xxx            │   模拟登录   │
└──────────┘                        └──────────────┘

替换后 (DolphinScheduler):
┌──────────┐     Token (Header)     ┌──────────────┐
│ system-  │ ─────────────────────→ │ DolphinSched │
│ service  │   /workflow-definitions │   (12345)    │
│ Schedule │   /executors/...        │              │
│ Service  │   /schedules/...        │   Token 鉴权 │
└──────────┘                        └──────────────┘
```

### 新建 `data-common-ds` 公共模块

```
data-common/
├── data-common-core/
├── data-common-jpa/
├── data-common-redis/
├── data-common-security/
├── data-common-xxljob/     ← 废弃
└── data-common-ds/         ← 新增：DolphinScheduler 客户端
    ├── pom.xml
    └── src/main/java/com/platform/common/ds/
        ├── config/
        │   └── DolphinSchedulerProperties.java    # @ConfigurationProperties
        ├── client/
        │   ├── DsProjectClient.java                # 项目管理
        │   ├── DsWorkflowClient.java               # 工作流 CRUD
        │   ├── DsTaskClient.java                   # 任务定义
        │   ├── DsScheduleClient.java               # 调度管理
        │   ├── DsExecutorClient.java               # 执行控制
        │   ├── DsDataSourceClient.java            # 数据源管理
        │   └── DsResourceClient.java              # 资源管理
        ├── model/
        │   ├── DsWorkflowDefinition.java
        │   ├── DsTaskDefinition.java
        │   ├── DsSchedule.java
        │   ├── DsWorkflowInstance.java
        │   └── DsResult.java                        # 统一响应包装
        └── service/
            └── DsTemplateService.java               # 封装常用操作
```

### 配置方式

各服务 `application.yml`：

```yaml
dolphinscheduler:
  api-url: http://localhost:12345/dolphinscheduler/api
  token: ${DS_TOKEN:your-ds-token}
  project-code: ${DS_PROJECT_CODE:1}
  default-worker-group: default
  default-tenant: default
```

### 与前端调度页面的变化

```
替换前 (JobConfig.vue):
┌─────────────┬──────────┬──────────┬────────┐
│ 任务名称     │ Cron编辑  │ 启停     │ 执行一次 │
└─────────────┴──────────┴──────────┴────────┘
→ 每行一个独立 XXL-Job 任务

替换后 (WorkflowConfig.vue):
┌─────────────┬──────────┬──────────┬────────┬──────────┐
│ 工作流名称   │ Cron编辑  │ 上线/下线 │ 执行一次│ 查看DAG   │
├─────────────┼──────────┼──────────┼────────┼──────────┤
│ 每日ETL     │ 0 0 2 * *│ 已上线   │ 执行   │ 查看 →    │
│ 数据质量检查 │ 0 30 6 * *│ 已上线  │ 执行   │ 查看 →    │
│ 实时同步     │ -        │ 运行中   │ -      │ 查看 →    │
└─────────────┴──────────┴──────────┴────────┴──────────┘
→ 每行一个 DAG 工作流（含多个任务和依赖关系）
→ "查看DAG" 跳转到 DolphinScheduler 内嵌的 DAG 可视化页面
```

## 五、OpenMetadata 替换当前血缘逻辑

### 架构变化

```
替换前 (data-lineage-service):
┌──────────────────────┐
│ data-lineage-service  │
│                      │
│ Druid SQL Parser     │ ← 手动解析 SQL AST
│ SqlLineageParser     │
│ TableLineage         │
│ FieldLineage         │
│                      │
│ 解析结果存入 Doris    │
│ 前端 ECharts 画图     │
└──────────────────────┘

替换后 (data-lineage-service):
┌──────────────────────┐           ┌──────────────────┐
│ data-lineage-service │  REST API  │  OpenMetadata    │
│                      │──────────→ │    (8585)        │
│ OpenMetadata 代理    │            │                  │
│ - 提交血缘           │            │ - 元数据存储     │
│ - 查询血缘图         │            │ - 血缘图引擎     │
│ - 注册表元数据       │            │ - 全文搜索       │
│ - 搜索元数据         │            │ - 数据质量       │
└──────────────────────┘           └──────────────────┘
```

### `data-lineage-service` 职责重新定义

```
data-lineage-service (8805)
├── controller/
│   ├── LineageController.java          # 血缘查询和提交
│   ├── MetadataController.java        # 元数据查询
│   └── SearchController.java          # 搜索代理
├── service/
│   ├── OmLineageService.java          # 调用 OM 血缘 API
│   ├── OmMetadataService.java         # 调用 OM 元数据 API
│   ├── OmSearchService.java           # 调用 OM 搜索 API
│   └── OmWebhookService.java          # 接收 OM Webhook 事件
└── client/
    └── OpenMetadataClient.java         # HTTP 客户端封装
```

### 前端页面变化

```
替换前 (/lineage/graph):
→ 调用后端解析 SQL，前端 ECharts 画血缘图

替换后 (/lineage/graph):
→ 嵌入 OpenMetadata 内置血缘图 UI（iframe）
→ 或调用 OM API 获取血缘数据，前端渲染
```

### 配置方式

`data-lineage-service/application.yml`：

```yaml
openmetadata:
  api-url: http://localhost:8585/api
  jwt-token: ${OM_JWT_TOKEN:your-om-token}
  # 用于 webhook 签名验证
  webhook-secret: ${OM_WEBHOOK_SECRET:your-webhook-secret}
```

## 六、Docker Compose 部署

新增两个服务到 `docker-compose.yml`：

```yaml
services:
  # ... 已有服务 ...

  dolphinscheduler:
    image: apache/dolphinscheduler-standalone-server:3.2.2
    container_name: dolphinscheduler
    ports:
      - "12345:12345"    # API + UI
    environment:
      - DS_DB_HOST=mysql-ds
      - DS_DB_PORT=3306
      - DS_DB_DATABASE=dolphinscheduler
      - DS_DB_USER=root
      - DS_DB_PASSWORD=ds_password
    depends_on:
      - mysql-ds
    networks:
      - data-platform

  mysql-ds:
    image: mysql:8.0
    container_name: mysql-ds
    environment:
      - MYSQL_ROOT_PASSWORD=ds_password
      - MYSQL_DATABASE=dolphinscheduler
    ports:
      - "3310:3306"  # 不对外暴露，仅内部网络
    volumes:
      - mysql-ds-data:/var/lib/mysql
    networks:
      - data-platform

  openmetadata:
    image: openmetadata/server:1.12.x
    container_name: openmetadata
    ports:
      - "8585:8585"
    environment:
      - OMServerHost=openmetadata
      - DATABASE_TYPE=mysql
      - DATABASE_HOST=mysql-om
      - DATABASE_PORT=3306
      - DATABASE_NAME=openmetadata
      - DATABASE_USER=root
      - DATABASE_PASSWORD=om_password
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_PORT=9200
    depends_on:
      - mysql-om
      - elasticsearch
    networks:
      - data-platform

  mysql-om:
    image: mysql:8.0
    container_name: mysql-om
    environment:
      - MYSQL_ROOT_PASSWORD=om_password
      - MYSQL_DATABASE=openmetadata
    ports:
      - "3311:3306"
    volumes:
      - mysql-om-data:/var/lib/mysql
    networks:
      - data-platform

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    networks:
      - data-platform

volumes:
  mysql-ds-data:
  mysql-om-data:
  es-data:
```

## 七、端口规划

| 组件 | 端口 | 对外暴露 | 说明 |
|------|------|:--------:|------|
| data-gateway | 8601 | ✅ | 唯一入口 |
| system-service | 8800 | ❌ | 127.0.0.1 |
| data-quality-service | 8801 | ❌ | 127.0.0.1 |
| data-realtime-service | 8802 | ❌ | 127.0.0.1 |
| data-integration-service | 8803 | ❌ | 127.0.0.1 |
| data-governance-service | 8804 | ❌ | 127.0.0.1 |
| data-lineage-service | 8805 | ❌ | 127.0.0.1 |
| DolphinScheduler | 12345 | ✅ | DAG UI 需要前端直接访问 |
| OpenMetadata | 8585 | ✅ | 元数据 UI 需要前端直接访问 |
| DolphinScheduler MySQL | 3310 | ❌ | 内部网络 |
| OpenMetadata MySQL | 3311 | ❌ | 内部网络 |
| Elasticsearch | 9200 | ❌ | 内部网络 |

> DolphinScheduler 和 OpenMetadata 的 UI 需要对外暴露，因为前端需要嵌入它们的可视化界面（DAG 编辑器、血缘图）。

## 八、迁移步骤

### Phase 1：基础设施搭建（不影响现有功能）

```
1. docker-compose 新增 DolphinScheduler + OpenMetadata + 依赖
2. 创建 data-common-ds 公共模块（DS 客户端封装）
3. 创建 data-common-om 公共模块（OM 客户端封装）
4. 各服务 application.yml 新增 DS/OM 配置
```

### Phase 2：调度迁移（并行运行，逐步切换）

```
1. system-service 新建 ScheduleService（调 DS API）
2. 前端新增 /schedule/workflows 页面（工作流列表）
3. 在 DS 中创建与 XXL-Job 对应的工作流定义
4. 逐步将任务从 XXL-Job 迁移到 DS
5. 确认所有任务在 DS 正常调度后，停用 XXL-Job
```

### Phase 3：元数据迁移（并行运行，逐步切换）

```
1. data-lineage-service 新增 OmClient（调 OM API）
2. 注册 Doris 数据源到 OpenMetadata
3. 注册所有表元数据到 OpenMetadata
4. 将历史血缘关系提交到 OpenMetadata
5. 前端血缘页面切换到 OM 数据源
6. 确认后停用 Druid SQL Parser 血缘解析
```

### Phase 4：清理

```
1. 移除 data-common-xxljob 模块
2. 移除 system-service 中的 JobConfigService（XXL-Job 调用）
3. 移除 data-lineage-service 中的 SqlLineageParser
4. 移除 docker-compose 中的 XXL-Job Admin 和 XXL-Job MySQL
5. 更新 README 文档
```

## 九、模块依赖图（升级后）

```
data-common/
├── data-common-core          # 基础工具
├── data-common-jpa           # JPA + Doris
├── data-common-redis          # Redis
├── data-common-security       # JWT 安全
├── data-common-ds             # DolphinScheduler 客户端 (新增)
└── data-common-om             # OpenMetadata 客户端 (新增)

各服务按需引入：
- system-service:          core + jpa + redis + security + ds
- data-quality-service:    core + jpa + redis + security + ds + om
- data-realtime-service:   core + jpa + redis + ds
- data-integration-service: core + jpa + redis + ds + om
- data-governance-service:  core + jpa + redis + security + ds + om
- data-lineage-service:    core + jpa + redis + security + om
```

> 每个服务只引入它实际需要的公共模块。DolphinScheduler 和 OpenMetadata 的客户端封装在 `data-common-ds` 和 `data-common-om` 中，通过 Maven 依赖引入，但**运行时通信只通过 HTTP REST API**，无源码级强依赖。
