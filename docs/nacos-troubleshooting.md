# Nacos 部署与排错总结

## 一、遇到的三个问题

### 问题 1：`No spring.config.import property has been defined`

**原因**：Spring Boot 3.x + Spring Cloud 2023.x 去掉了 `bootstrap.yml` 支持，必须显式声明 `spring.config.import` 才能启用 Nacos 配置中心。

**修复**：在每个微服务的 `application.yml` 中添加：

```yaml
spring:
  config:
    import: optional:nacos:服务名.yml
```

- `optional:` 前缀 — Nacos 连不上也不阻止启动，用本地配置兜底
- `nacos:服务名.yml` — 从 Nacos 配置中心拉取的配置文件名
- 每个服务的 import 路径不同：data-auth 引用 `data-auth.yml`，data-gateway 引用 `data-gateway.yml`

### 问题 2：`Client not connected, current status:STARTING`

**原因**：Nacos 2.x 用 gRPC 通信，gRPC 端口 = HTTP 端口 + 1000。K8s NodePort 映射后端口偏移不对齐。

**端口计算规则**：
```
Nacos 客户端 HTTP 端口:  server-addr 指定的端口
Nacos 客户端 gRPC 端口:  HTTP 端口 + 1000 (自动计算，无法覆盖)
```

**错误的配置（端口不对齐）**：
```
K8s Service:
  8848 (HTTP) → NodePort 30849
  9848 (gRPC) → NodePort 31848  ← 差了1！

application.yml:
  server-addr: 192.168.81.104:30849
  
客户端计算: gRPC = 30849 + 1000 = 31849
实际 NodePort: 31848  ← 连不上
```

**修复：把 gRPC 的 NodePort 从 31848 改成 31849**

```bash
kubectl edit svc nacos-svc -n my-ns
```

```yaml
spec:
  type: NodePort
  ports:
  - port: 8080          # Console Web UI
    targetPort: 8080
    nodePort: 30848
    name: web-console
  - port: 8848          # Client HTTP API
    targetPort: 8848
    nodePort: 30849
    name: client-http
  - port: 9848          # gRPC
    targetPort: 9848
    nodePort: 31849      # ← 必须是 30849 + 1000
    name: rpc
```

### 问题 3：Nacos 版本和镜像

**Nacos 没有 v3.2.3 版本**，正确版本是 `v2.3.2`：

```yaml
image: docker.io/nacos/nacos-server:v2.3.2   # 不是 v3.2.3
```

**国内拉取镜像**（Docker Hub 被墙）：

```bash
# 配置 RKE2 镜像源（所有节点）
mkdir -p /etc/rancher/rke2
cat > /etc/rancher/rke2/registries.yaml << 'EOF'
mirrors:
  docker.io:
    endpoint:
      - "https://docker.m.daocloud.io"
      - "https://registry.cn-hangzhou.aliyuncs.com"
EOF

# 重启 rke2
systemctl restart rke2-server   # master 节点
systemctl restart rke2-agent    # worker 节点
```

## 二、Nacos K8s 部署完整 YAML

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nacos
  namespace: my-ns
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nacos
  template:
    metadata:
      labels:
        app: nacos
    spec:
      containers:
      - name: nacos
        image: docker.io/nacos/nacos-server:v2.3.2
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080   # Console
        - containerPort: 8848   # Client HTTP
        - containerPort: 9848   # gRPC
        env:
        - name: MODE
          value: "standalone"
        - name: PREFER_HOST_MODE
          value: "hostname"
        - name: JVM_XMS
          value: "512m"
        - name: JVM_XMX
          value: "512m"
        - name: SPRING_DATASOURCE_PLATFORM
          value: "mysql"
        - name: MYSQL_SERVICE_HOST
          value: "mysql-svc.my-ns.svc.cluster.local"
        - name: MYSQL_SERVICE_PORT
          value: "3306"
        - name: MYSQL_SERVICE_DB_NAME
          value: "nacos_config"
        - name: MYSQL_SERVICE_USER
          value: "root"
        - name: MYSQL_SERVICE_PASSWORD
          value: "Root@123456"
        - name: NACOS_AUTH_ENABLE
          value: "true"
        - name: NACOS_AUTH_TOKEN
          value: "SecretKey012345678901234567890123456789012345678901234567890123456789"
        - name: NACOS_AUTH_IDENTITY_KEY
          value: "SecretKey01234567890123456789012345678901234567890123456789"
        - name: NACOS_AUTH_IDENTITY_VALUE
          value: "SecretValue01234567890123456789012345678901234567890123456789"
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 1000m
            memory: 2Gi
---
apiVersion: v1
kind: Service
metadata:
  name: nacos-svc
  namespace: my-ns
spec:
  type: NodePort
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30848
    name: web-console
  - port: 8848
    targetPort: 8848
    nodePort: 30849
    name: client-http
  - port: 9848
    targetPort: 9848
    nodePort: 31849          # 必须 = 30849 + 1000
    name: rpc
  selector:
    app: nacos
```

## 三、Nacos 端口对应关系

| 内部端口 | NodePort | 用途 | 谁访问 |
|---------|----------|------|--------|
| 8080 | 30848 | Web 控制台 | 浏览器访问 `http://IP:30848/` |
| 8848 | 30849 | Client HTTP API | 微服务 `server-addr` 指向这里 |
| 9848 | 31849 | gRPC | Nacos 客户端自动连接（= HTTP端口 + 1000） |

## 四、微服务 application.yml 正确模板

### data-auth（端口 8603）

```yaml
server:
  address: 127.0.0.1
  port: 8603

spring:
  application:
    name: data-auth
  config:
    import: optional:nacos:data-auth.yml          # ← 必须有
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.81.104:30306/data_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
    username: root
    password: Root@123456
    type: com.alibaba.druid.pool.DruidDataSource
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.81.104:30849          # ← HTTP 端口
        username: nacos
        password: nacos
      config:
        server-addr: 192.168.81.104:30849          # ← 同上
        file-extension: yml

jwt:
  secret: data-platform-secret-key-must-be-at-least-32-characters-long
  expiration-ms: 86400000
```

### data-gateway（端口 8601）

```yaml
server:
  port: 8601

spring:
  application:
    name: data-gateway
  config:
    import: optional:nacos:data-gateway.yml        # ← 必须有
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: data-auth
          uri: lb://data-auth
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=1
        # ... 其他路由
    nacos:
      discovery:
        server-addr: 192.168.81.104:30849          # ← HTTP 端口
      config:
        server-addr: 192.168.81.104:30849          # ← 同上
        file-extension: yml

jwt:
  secret: data-platform-secret-key-must-be-at-least-32-characters-long

gateway:
  white-list:
    - /auth/login
    - /auth/register
    - /actuator/**
    - /doc.html
    - /webjars/**
    - /v3/api-docs/**
```

### system-service（端口 8800）

```yaml
server:
  address: 127.0.0.1
  port: 8800

spring:
  application:
    name: system-service
  config:
    import: optional:nacos:system-service.yml     # ← 必须有
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.81.104:30306/data_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
    username: root
    password: Root@123456
    type: com.alibaba.druid.pool.DruidDataSource
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.81.104:30849          # ← HTTP 端口
      config:
        server-addr: 192.168.81.104:30849          # ← 同上
        file-extension: yml
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

dolphinscheduler:
  api-url: http://localhost:12345/dolphinscheduler/api
  token: default_token
  project-code: 1
  default-worker-group: default
  default-tenant: default
```

## 五、Nacos 数据库初始化

Nacos 用 MySQL 存储配置，需要先建库建表：

```bash
# 连接 MySQL
kubectl exec -it <mysql-pod> -n my-ns -- mysql -uroot -pRoot@123456

# 建库
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 导入表结构（从 Nacos 镜像提取或从 GitHub 下载）
# GitHub: https://raw.githubusercontent.com/alibaba/nacos/2.3.2/distribution/conf/mysql-schema.sql
SOURCE /tmp/mysql-schema.sql;

# 验证
USE nacos_config;
SHOW TABLES;
```

## 六、启动顺序

```
1. MySQL（端口 3306）          ← Nacos 和业务库的存储
2. Redis（端口 6379）           ← JWT token 存储
3. Nacos（端口 30849/31849）   ← 服务注册发现 + 配置中心
4. 初始化 data_platform 库     ← 执行 mysql_ddl.sql
5. data-auth（端口 8603）       ← IDEA 启动 DataAuthApplication
6. data-gateway（端口 8601）    ← IDEA 启动 DataGatewayApplication
7. 前端 npm run dev（端口 9528）← 访问 http://localhost:9528
```

## 七、验证 Nacos 是否正常

```bash
# 浏览器访问控制台
http://192.168.81.104:30848/
# 账号: nacos / nacos

# 测试 HTTP API
curl http://192.168.81.104:30849/nacos/v1/ns/operator/metrics

# 在 K8s 里看 Nacos 服务列表
kubectl get svc -n my-ns | grep nacos

# 看微服务是否注册成功
# 启动 data-auth 后，在 Nacos 控制台 → 服务管理 → 服务列表 看到 data-auth
```

## 八、常见错误速查

| 错误信息 | 原因 | 修复 |
|---------|------|------|
| `No spring.config.import property has been defined` | 缺少 config.import 配置 | 加 `spring.config.import: optional:nacos:服务名.yml` |
| `Client not connected, current status:STARTING` | gRPC 端口不对齐 | gRPC NodePort = HTTP NodePort + 1000 |
| `connection is unauthorized: Unauthorized` | Nacos 认证配置缺失 | 添加 NACOS_AUTH_TOKEN 等环境变量 |
| `namespaceMigratePreCheck failed` | Nacos 2.3.x 迁移检查 | 加 `nacos.conf.migration.enabled=false` |
| `No static resource nacos.` | 访问 Console 端口带了 `/nacos/` 路径 | Console 直接访问 `http://IP:30848/` |
| `Received HTTP/0.9 when not allowed` | 用 curl 访问 gRPC 端口 | gRPC 不是 HTTP，curl 报错是正常的 |

