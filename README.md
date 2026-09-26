# AI 智能数据中台

面向数据接入、数据开发、数据质量、数据治理与资产检索的一体化数据中台。

## 能力范围

```text
数据源 → 接入任务 → Doris
                    ├─ 质量规则 / 检测报告 / 不合格明细
                    ├─ 开发任务 / 工作流 / 调度实例
                    └─ OpenMetadata 资产 / 血缘 / 影响分析
```

- 数据接入：数据源配置、连接测试、全量/增量同步、字段映射和运行记录。
- 数据开发：SQL 任务、工作流编排、前置依赖和运行实例。
- 数据质量：表字段与跨表规则、手动/定时检测、质量报告和问题明细。
- 数据治理：结构对比、动态抽取、资产权限和血缘关联。
- 数据资产：通过 OpenMetadata 浏览表、字段、标签、术语与血缘。
- 调度：通过 DolphinScheduler 配置周期、重试、补数与运行日志。

## 项目结构

```text
data-platform-pro/
├── data-ui/                         Vue 3 前端
├── data-gateway/                    API 网关 :8601
├── data-auth/                       JWT 认证 :8603
├── data-common/                     公共能力
├── data-modules/
│   ├── system-service/              系统、开发任务和工作流 :8800
│   ├── data-quality-service/        质量规则和报告 :8801
│   ├── data-realtime-service/       实时写入 :8802
│   ├── data-integration-service/    数据源与接入任务 :8803
│   ├── data-governance-service/     治理和结构同步 :8804
│   └── data-lineage-service/        血缘集成 :8805
├── docker/compose/                  基础设施编排
├── sql/doris_ddl.sql                Doris 单机初始化脚本
└── bushuqianyan/                    产品与架构说明
```

## 技术栈

| 范围 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Element Plus、ECharts |
| 后端 | Java 21、Spring Boot 3.2、Spring Cloud、JPA、Druid |
| 网关与认证 | Spring Cloud Gateway、LoadBalancer、JWT、Nacos |
| 分析数据 | Apache Doris |
| 资产与血缘 | OpenMetadata |
| 调度 | Apache DolphinScheduler |
| 缓存 | Redis |

## 本地启动

### 1. 基础设施

```bash
cd docker/compose
docker compose -f docker-compose.core.yml up -d
docker compose -f docker-compose.scheduler.yml up -d
docker compose -f docker-compose.metadata.yml up -d
```

### 2. 初始化 Doris

```bash
mysql -h 192.168.81.104 -P 9030 -u root -proot < sql/doris_ddl.sql
```

### 3. 启动服务

推荐顺序：data-auth、system-service、data-integration-service、data-quality-service、data-governance-service、data-lineage-service、data-realtime-service、data-gateway。

启动后在 Nacos 控制台确认全部实例健康，再启动前端。

### 4. 启动前端

```bash
cd data-ui
npm install
npm run dev
```

访问 `http://localhost:9528`。

## 关键地址

| 服务 | 地址 |
| --- | --- |
| 前端 | http://localhost:9528 |
| Gateway | http://localhost:8601 |
| Nacos | http://192.168.81.104:8848/nacos |
| Doris FE | 192.168.81.104:9030 |
| OpenMetadata | http://192.168.81.104:8585 |
| DolphinScheduler | http://192.168.81.104:12345 |

## 文档

- [功能说明](bushuqianyan/文档1.yml)
- [架构与部署说明](bushuqianyan/文档2.txt)
- [Docker 部署说明](outputs/data-platform-docker-deployment.md)
- [任务依赖设计](docs/task-dependency-design.md)
- [OpenMetadata API 集成](docs/openmetadata-api.md)
