# 网关架构与服务间通信说明

## 一、网关统一入口

### 架构总览

```
外部请求 (浏览器/Mobile)
      │
      ▼
┌─────────────────────────────────────┐
│  data-gateway (端口 8601, 0.0.0.0)  │
│  ┌───────────────────────────────┐  │
│  │  GatewayJwtAuthFilter         │  │
│  │  - 白名单路径直接放行          │  │
│  │  - 其他路径验证 JWT Token     │  │
│  │  - 验证通过：注入 X-User-Id   │  │
│  │  - 验证失败：返回 401         │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  路由表 (StripPrefix=1)      │  │
│  │  /auth/**    → data-auth     │  │
│  │  /system/**  → system-service│  │
│  │  /quality/** → quality-service│  │
│  │  /realtime/**→ realtime-service│ │
│  │  /integration/**→ integration│  │
│  │  /governance/**→ governance  │  │
│  │  /lineage/** → lineage-service│ │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
      │  Nacos 服务发现 (lb://)
      ▼
┌──────────────────────────────────────┐
│  各微服务 (127.0.0.1 + 各自端口)     │
│  - 只绑定本地回环地址                │
│  - 外部无法直接访问                  │
│  - 仅网关可路由到这些服务             │
└──────────────────────────────────────┘
```

### 端口绑定策略

| 服务 | 绑定地址 | 端口 | 对外可访问 |
|------|---------|------|-----------|
| data-gateway | `0.0.0.0` (默认) | 8601 | 是，唯一入口 |
| data-auth | `127.0.0.1` | 8603 | 否 |
| system-service | `127.0.0.1` | 8800 | 否 |
| data-quality-service | `127.0.0.1` | 8801 | 否 |
| data-realtime-service | `127.0.0.1` | 8802 | 否 |
| data-integration-service | `127.0.0.1` | 8803 | 否 |
| data-governance-service | `127.0.0.1` | 8804 | 否 |
| data-lineage-service | `127.0.0.1` | 8805 | 否 |

每个微服务的 `application.yml` 中配置：

```yaml
server:
  address: 127.0.0.1  # 只监听本地回环，外部无法直连
  port: 8800
```

> **生产环境**：将微服务部署在 Docker 内部网络中，仅网关端口映射到宿主机，实现网络隔离。

### 网关 JWT 鉴权

网关通过 `GatewayJwtAuthFilter`（`GlobalFilter`）在路由前统一验证 JWT：

**白名单路径**（不需要 Token）：
- `/auth/login` — 登录
- `/auth/register` — 注册
- `/actuator/**` — 健康检查
- `/doc.html`, `/webjars/**`, `/v3/api-docs/**` — API 文档

**鉴权流程**：
1. 请求到达网关
2. 判断路径是否在白名单 → 是则放行
3. 提取 `Authorization: Bearer <token>` 头
4. 用与 `data-auth` 相同的密钥验证 Token
5. 验证通过 → 注入 `X-User-Id` 和 `X-Username` 请求头，转发到后端服务
6. 验证失败 → 返回 HTTP 401

> **后端服务无需再做 JWT 验证**，直接从 `X-User-Id` / `X-Username` 请求头获取当前用户。

---

## 二、服务间通信方式

### 结论：服务间调用不走网关，走 Feign + Nacos

**为什么不走网关？**

| 问题 | 说明 |
|------|------|
| 多一跳网络延迟 | A → 网关 → B，比 A → B 多一次路由转发 |
| 循环依赖 | 网关依赖服务路由表，服务又依赖网关转发，形成环 |
| 重复鉴权 | 网关 JWT 过滤器会对内部调用也做鉴权，内部调用已经可信 |

### 正确方式：Feign + Nacos 服务发现

```
┌──────────────┐
│ Service A    │
│ (8800)       │
│              │  @FeignClient(name = "data-quality-service")
│  Feign Client├──────┐
└──────────────┘      │
                      │  Nacos 服务发现
                      │  解析 data-quality-service → 127.0.0.1:8801
                      ▼
                ┌──────────────┐
                │ Service B    │
                │ (8801)       │
                │              │
                └──────────────┘
```

**工作原理**：
1. 服务启动时注册到 Nacos（`spring.cloud.nacos.discovery`）
2. Service A 用 `@FeignClient(name = "data-quality-service")` 声明 Feign 接口
3. Feign 从 Nacos 拿到 `data-quality-service` 的实际地址（`127.0.0.1:8801`）
4. 直接 HTTP 调用，不经过网关
5. Spring Cloud LoadBalancer 负责负载均衡（多实例时）

### Feign 使用示例

**1. 声明 Feign 接口**（放在 `data-common-core` 或调用方的模块中）：

```java
@FeignClient(name = "data-quality-service", path = "/quality/api")
public interface QualityFeignClient {

    @GetMapping("/rules/{id}")
    R<QualityRule> getRule(@PathVariable("id") String id);

    @PostMapping("/rules/execute")
    R<String> executeRule(@RequestBody Map<String, Object> params);
}
```

**2. 在主启动类上启用 Feign**（已有 `@EnableFeignClients` 的服务无需修改）：

```java
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.platform")
public class SystemApplication { ... }
```

**3. 注入并调用**：

```java
@RequiredArgsConstructor
public class SomeService {
    private final QualityFeignClient qualityFeignClient;

    public void doSomething() {
        R<QualityRule> result = qualityFeignClient.getRule("rule-001");
    }
}
```

### 当前已启用 Feign 的服务

| 服务 | `@EnableFeignClients` | 实际 `@FeignClient` 接口 |
|------|----------------------|-------------------------|
| data-auth | 是 | 暂无 |
| system-service | 是 | 暂无 |
| data-governance-service | 是 | 暂无 |
| data-quality-service | 是 | 暂无 |

> Feign 基础设施已就绪，按需添加 `@FeignClient` 接口即可。

---

## 三、YAML 配置规范

### 配置归属原则

| 配置类型 | 放在哪个文件 | 原因 |
|---------|------------|------|
| 网关路由 (`spring.cloud.gateway.routes`) | `data-gateway/application.yml` | 网关专属 |
| 网关白名单 (`gateway.white-list`) | `data-gateway/application.yml` | 网关专属 |
| JWT 密钥 (`jwt.secret`) | `data-gateway` + `data-auth` | 两处需要，使用相同值 |
| 数据源 (`spring.datasource`) | 各服务自己的 `application.yml` | 每个服务独立连接数据库 |
| Redis (`spring.data.redis`) | 各服务自己的 `application.yml` | 每个服务独立连接 Redis |
| Nacos (`spring.cloud.nacos`) | 各服务自己的 `application.yml` | 注册和配置中心地址 |
| Doris 连接 (`doris.*`) | 需要连接 Doris 的服务 | 网关不需要，已删除 |
| XXL-Job (`xxl.job.*`) | 注册为执行器的服务 | 不需要执行器的服务不配置 |
| 连接池 (`druid.*`) | 使用 Druid 的服务 | 池参数可按服务负载调整 |

### 已清理的配置

| 文件 | 清理内容 | 原因 |
|------|---------|------|
| `data-gateway/application.yml` | 删除 `doris:` 配置块 | 网关是纯路由层，不连接数据库 |
| `data-gateway/application.yml` | 新增 `jwt.secret` 和 `gateway.white-list` | 网关鉴权需要 |
| 所有 7 个微服务 `application.yml` | 新增 `server.address: 127.0.0.1` | 隐藏端口，仅网关对外 |

### 生产环境建议

将重复的公共配置（数据源、Redis、Nacos 地址）放到 **Nacos 配置中心**，通过 `spring.cloud.nacos.config` 拉取共享配置，减少各服务 YAML 的重复内容：

```yaml
# Nacos 配置中心共享配置示例 (data-platform-common.yml)
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://doris-fe:9030/data_platform?...
    username: ${DORIS_USER}
    password: ${DORIS_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
```

各服务只需在 `bootstrap.yml` 中指定 Nacos 配置：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_ADDR}
        file-extension: yml
        shared-configs:
          - data-platform-common.yml
```
