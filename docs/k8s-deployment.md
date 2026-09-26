# Kubernetes 部署指南

> 企业级数据中台 K8s 部署文档
> 技术栈: JDK 21 + Spring Boot 3.2 + Spring Cloud + Doris + DolphinScheduler + OpenMetadata + Nacos + Redis

---

## 一、架构总览

```
                        ┌──────────────────────────────────────────┐
                        │            Ingress (nginx)               │
                        │   data-platform.local                     │
                        └──────────────────┬───────────────────────┘
                           │                │           │
            ┌──────────────┴──┐  ┌─────────┴──────────┐ │
            │  data-gateway    │  │  DolphinScheduler  │ │
            │  (NodePort)      │  │  Web UI            │ │
            │  :8601           │  │  :12345            │ │
            └──────┬───────────┘  └────────────────────┘ │
                   │                       ┌─────────────┘
     ┌─────────────┼─────────────┐        │
     │             │             │   ┌────┴──────────┐
┌────┴────┐ ┌─────┴─────┐ ┌─────┴──┐ │ OpenMetadata  │
│data-auth │ │system-svc │ │lineage│ │ Web UI        │
│:8603    │ │:8800      │ │:8805  │ │ :8586         │
└─────────┘ └───────────┘ └───────┘ └───────────────┘
     │             │             │           │
     └─────────────┴─────────────┴───────────┘
                        │
           ┌────────────┼────────────┐
           │            │            │
    ┌──────┴──┐  ┌──────┴──┐  ┌─────┴──┐
    │ Doris   │  │ Nacos   │  │ Redis  │
    │ FE/BE   │  │ :8848   │  │ :6379  │
    │ :9030   │  │         │  │        │
    └─────────┘  └─────────┘  └────────┘
           │
    ┌──────┴──────────┐
    │ DolphinScheduler │    PostgreSQL + Elasticsearch
    │ :12345           │    (OpenMetadata 依赖)
    └──────────────────┘
```

### 端口规划

| 服务 | 容器端口 | Service 类型 | 说明 |
|------|---------|-------------|------|
| data-gateway | 8601 | NodePort (30601) | 唯一对外入口 |
| data-auth | 8603 | ClusterIP | 认证服务 |
| system-service | 8800 | ClusterIP | 系统管理 + 调度管理 |
| data-quality-service | 8801 | ClusterIP | 数据质量 |
| data-realtime-service | 8802 | ClusterIP | 实时计算 |
| data-integration-service | 8803 | ClusterIP | 数据集成 |
| data-governance-service | 8804 | ClusterIP | 数据治理 |
| data-lineage-service | 8805 | ClusterIP | 数据血缘 |
| nacos | 8848 | ClusterIP | 注册中心 |
| doris-fe | 8030, 9030 | ClusterIP | Doris 前端 |
| doris-fe | 8030, 9030, 8040, 9050, 8060 | ClusterIP | Doris All-in-One |
| redis | 6379 | ClusterIP | 缓存 |
| dolphinscheduler | 12345 | ClusterIP | 调度引擎 |
| postgres-om | 5432 | ClusterIP | OM 元数据库 |
| elasticsearch-om | 9200, 9300 | ClusterIP | OM 搜索引擎 |
| openmetadata | 8585, 8586 | ClusterIP | 元数据管理 |

---

## 二、前置条件

### 2.1 K8s 集群要求

- Kubernetes >= 1.28
- 至少 3 个 worker 节点（推荐配置: 4C 8G per node）
- 已安装 Metrics Server
- 已配置默认 StorageClass（用于动态 PVC 供给）
- 已安装 Nginx Ingress Controller

### 2.2 客户端工具

```bash
# 检查 kubectl 版本
kubectl version --client

# 检查集群状态
kubectl get nodes
kubectl get storageclass
```

### 2.3 安装 Nginx Ingress Controller（如未安装）

```bash
# Helm 方式安装
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=LoadBalancer
```

---

## 三、构建微服务镜像

### 3.1 统一 Dockerfile

项目根目录 `docker/Dockerfile` 为所有微服务通用的构建模板:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m"
ENV SERVER_ADDRESS="0.0.0.0"
EXPOSE 8800
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.address=$SERVER_ADDRESS -jar /app/app.jar"]
```

> **关键**: K8s 环境中 `SERVER_ADDRESS` 必须设为 `0.0.0.0`，与 Docker Compose 环境中的 `127.0.0.1` 不同。K8s Pod 有独立网络命名空间，绑定 0.0.0.0 不会暴露端口，Service 和 NetworkPolicy 负责网络隔离。

### 3.2 构建脚本

```bash
#!/bin/bash
# build-images.sh - 构建所有微服务镜像

REGISTRY=${REGISTRY:-"data-platform"}
VERSION=${VERSION:-"1.0.0"}

# 先在项目根目录执行 Maven 打包
mvn clean package -DskipTests

# 构建各服务镜像
services=(
  "data-gateway:8601"
  "data-auth:8603"
  "data-modules/system-service:8800"
  "data-modules/data-quality-service:8801"
  "data-modules/data-realtime-service:8802"
  "data-modules/data-integration-service:8803"
  "data-modules/data-governance-service:8804"
  "data-modules/data-lineage-service:8805"
)

for entry in "${services[@]}"; do
  IFS=':' read -r module port <<< "$entry"
  name=$(basename "$module")
  echo "构建 $name (port: $port)..."
  
  docker build -f docker/Dockerfile \
    --build-arg JAVA_OPTS="-Xms256m -Xmx512m" \
    -t "$REGISTRY/$name:$VERSION" \
    "$module"
  
  # 如果有私有仓库，推送镜像
  if [ -n "$PUSH_REGISTRY" ]; then
    docker push "$PUSH_REGISTRY/$name:$VERSION"
  fi
done

echo "所有镜像构建完成"
```

### 3.3 构建并推送

```bash
# 本地构建
chmod +x docker/build-images.sh
./docker/build-images.sh

# 推送到私有仓库（如需要）
export PUSH_REGISTRY=harbor.your-company.com
docker login $PUSH_REGISTRY
./docker/build-images.sh

# 如果使用私有仓库，修改 20-microservices.yaml 中的 image 字段
# 例如: data-platform/data-gateway:1.0.0 → harbor.your-company.com/data-gateway:1.0.0
```

---

## 四、部署步骤

### 4.1 部署顺序

```
00-namespace → 01-secret → 02-configmap
     ↓
基础设施:
  10-nacos → 11-redis → 12-doris
  13-postgres-elasticsearch → 14-dolphinscheduler → 15-openmetadata
     ↓
微服务:
  20-microservices
     ↓
入口:
  30-ingress
```

### 4.2 一键部署

```bash
# 进入 K8s YAML 目录
cd docker/k8s

# 按顺序应用所有 YAML
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-secret.yaml
kubectl apply -f 02-configmap.yaml

# 等待基础设施就绪
kubectl apply -f 10-nacos.yaml
kubectl apply -f 11-redis.yaml
kubectl apply -f 12-doris.yaml
kubectl apply -f 13-postgres-elasticsearch.yaml

# 等待 Nacos 就绪
kubectl wait --for=condition=ready pod -l app=nacos -n data-platform --timeout=120s

# 等待 Doris FE 就绪
kubectl wait --for=condition=ready pod -l app=doris-fe -n data-platform --timeout=180s

# 部署 DolphinScheduler
kubectl apply -f 14-dolphinscheduler.yaml

# 等待 PostgreSQL 和 Elasticsearch 就绪
kubectl wait --for=condition=ready pod -l app=postgres-om -n data-platform --timeout=120s
kubectl wait --for=condition=ready pod -l app=elasticsearch-om -n data-platform --timeout=180s

# 部署 OpenMetadata
kubectl apply -f 15-openmetadata.yaml
kubectl wait --for=condition=ready pod -l app=openmetadata -n data-platform --timeout=180s

# 部署微服务
kubectl apply -f 20-microservices.yaml

# 部署 Ingress
kubectl apply -f 30-ingress.yaml
```

### 4.3 分步部署（推荐）

#### 步骤 1: 创建命名空间和配置

```bash
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-secret.yaml
kubectl apply -f 02-configmap.yaml
```

#### 步骤 2: 部署 Nacos + Redis

```bash
kubectl apply -f 10-nacos.yaml
kubectl apply -f 11-redis.yaml

# 验证
kubectl get pods -n data-platform -l app=nacos
kubectl get pods -n data-platform -l app=redis
```

#### 步骤 3: 部署 Doris

```bash
kubectl apply -f 12-doris.yaml

# Doris 启动较慢，请耐心等待
kubectl get pods -n data-platform -l 'app in (doris-fe,doris-be)' -w
```

#### 步骤 4: 部署 DolphinScheduler

```bash
kubectl apply -f 14-dolphinscheduler.yaml
kubectl get pods -n data-platform -l app=dolphinscheduler -w
```

#### 步骤 5: 部署 OpenMetadata 依赖

```bash
kubectl apply -f 13-postgres-elasticsearch.yaml

# 等待就绪
kubectl wait --for=condition=ready pod -l app=postgres-om -n data-platform --timeout=120s
kubectl wait --for=condition=ready pod -l app=elasticsearch-om -n data-platform --timeout=180s
```

#### 步骤 6: 部署 OpenMetadata

```bash
kubectl apply -f 15-openmetadata.yaml
kubectl get pods -n data-platform -l app=openmetadata -w
```

#### 步骤 7: 部署微服务

```bash
kubectl apply -f 20-microservices.yaml

# 查看所有 Pod
kubectl get pods -n data-platform
```

#### 步骤 8: 部署 Ingress

```bash
kubectl apply -f 30-ingress.yaml

# 获取 Ingress 地址
kubectl get ingress -n data-platform
```

---

## 五、配置说明

### 5.1 环境变量覆盖

K8s 中通过环境变量覆盖 `application.yml` 中的默认值。Spring Boot 的 relaxed binding 支持以下映射:

| 环境变量 | 对应配置项 | 说明 |
|---------|-----------|------|
| `SERVER_ADDRESS` | `server.address` | K8s 中必须为 `0.0.0.0` |
| `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR` | `spring.cloud.nacos.discovery.server-addr` | K8s Service DNS |
| `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR` | `spring.cloud.nacos.config.server-addr` | K8s Service DNS |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | Doris FE 地址 |
| `SPRING_DATA_REDIS_HOST` | `spring.data.redis.host` | Redis Service 地址 |
| `DOLPHINSCHEDULER_API_URL` | `dolphinscheduler.api-url` | DS API 地址 |
| `OPENMETADATA_API_URL` | `openmetadata.api-url` | OM API 地址 |
| `OPENMETADATA_JWT_TOKEN` | `openmetadata.jwt-token` | OM 认证 Token |

### 5.2 Doris 初始化

Doris FE 启动后需要初始化数据库和添加 BE 节点:

```bash
# 进入 Doris FE Pod
kubectl exec -it -n data-platform doris-fe-0 -- bash

# 登录 Doris（默认无密码）
mysql -h 127.0.0.1 -P 9030 -u root

# 创建业务数据库
CREATE DATABASE data_platform;

# 查看 BE 节点状态
SHOW BACKENDS;

# 如果 BE 未自动加入，手动添加
ALTER SYSTEM ADD BACKEND "doris-be-0.doris-be.data-platform.svc.cluster.local:9050";
```

### 5.3 DolphinScheduler 初始化

DS standalone 首次启动后需要:

1. 访问 `http://<ds-svc>:12345` 进入 Web UI
2. 默认账号: `admin` / `dolphinscheduler123`
3. 创建项目（project code 需与 ConfigMap 中一致）
4. 生成 API Token: 安全中心 → Token 管理

```bash
# 更新 Token 到 Secret
kubectl create secret generic platform-secrets \
  --from-literal=ds-token=<your-real-token> \
  --dry-run=client -o yaml | kubectl apply -f -
```

### 5.4 OpenMetadata 初始化

OM 首次启动后:

1. 访问 `http://<om-svc>:8586` 进入 Web UI
2. 注册管理员账号
3. 生成 JWT Token 用于 API 调用
4. 在 OM 中创建 Service 连接到 Doris

```bash
# 更新 JWT Token 到 Secret
kubectl create secret generic platform-secrets \
  --from-literal=om-jwt-token=<your-real-jwt> \
  --dry-run=client -o yaml | kubectl apply -f -
```

---

## 六、服务访问

### 6.1 通过 Ingress 访问

首先配置本地 hosts 或 DNS:

```bash
# 获取 Ingress Controller 的外部 IP
kubectl get svc -n ingress-nginx ingress-nginx-controller

# 添加 hosts 记录
echo "<ingress-external-ip>  data-platform.local" | sudo tee -a /etc/hosts
```

访问地址:

| 服务 | URL | 说明 |
|------|-----|------|
| API 网关 | `http://data-platform.local/api` | 业务 API 入口 |
| DolphinScheduler | `http://data-platform.local/dolphinscheduler` | 调度管理 UI |
| OpenMetadata | `http://data-platform.local/openmetadata` | 元数据管理 UI |
| Doris Web | `http://data-platform.local/doris` | Doris 管理 UI |
| Nacos | `http://data-platform.local/nacos` | 服务注册/配置中心 |

### 6.2 通过 NodePort 访问

如果未配置 Ingress，可通过 NodePort 直接访问网关:

```bash
# 获取任意 Node IP
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
echo "API 网关: http://$NODE_IP:30601/api"
```

### 6.3 端口转发调试

```bash
# 转发网关端口
kubectl port-forward -n data-platform svc/data-gateway 8601:8601

# 转发 DolphinScheduler
kubectl port-forward -n data-platform svc/dolphinscheduler 12345:12345

# 转发 OpenMetadata
kubectl port-forward -n data-platform svc/openmetadata 8586:8586

# 转发 Doris FE
kubectl port-forward -n data-platform svc/doris-fe 9030:9030
kubectl port-forward -n data-platform svc/doris-fe 8030:8030
```

---

## 七、运维管理

### 7.1 查看服务状态

```bash
# 所有 Pod 状态
kubectl get pods -n data-platform -o wide

# 所有 Service
kubectl get svc -n data-platform

# 所有 PVC
kubectl get pvc -n data-platform

# 查看某服务日志
kubectl logs -n data-platform -l app=system-service -f --tail=200
```

### 7.2 扩缩容

```bash
# 扩容 data-gateway 到 3 个副本
kubectl scale deployment data-gateway -n data-platform --replicas=3

# 扩容 system-service
kubectl scale deployment system-service -n data-platform --replicas=2
```

### 7.3 滚动更新

```bash
# 更新镜像版本
kubectl set image deployment/system-service \
  system-service=harbor.your-company.com/system-service:1.1.0 \
  -n data-platform

# 查看滚动更新状态
kubectl rollout status deployment/system-service -n data-platform

# 回滚
kubectl rollout undo deployment/system-service -n data-platform
```

### 7.4 持久化存储

```bash
# 查看 PV
kubectl get pv | grep data-platform

# 查看 PVC 绑定状态
kubectl get pvc -n data-platform

# 存储清单
# Nacos: 2Gi (logs)
# Redis: 2Gi (data)
# Doris FE: 10Gi (meta) + 5Gi (log)
# Doris BE: 50Gi (storage) + 5Gi (log)
# DolphinScheduler: 5Gi (logs) + 5Gi (data)
# PostgreSQL: 10Gi (data)
# Elasticsearch: 10Gi (data)
# OpenMetadata: 5Gi (logs)
# 总计: ~96Gi
```

---

## 八、YAML 文件清单

```
docker/k8s/
├── 00-namespace.yaml              # 命名空间 + 资源配额
├── 01-secret.yaml                 # 密码和 Token
├── 02-configmap.yaml              # 共享配置
├── 10-nacos.yaml                  # Nacos 注册中心
├── 11-redis.yaml                  # Redis 缓存
├── 12-doris.yaml                  # Doris FE/BE (StatefulSet)
├── 13-postgres-elasticsearch.yaml # OM 依赖: PostgreSQL + ES
├── 14-dolphinscheduler.yaml       # 调度引擎
├── 15-openmetadata.yaml           # 元数据管理
├── 20-microservices.yaml           # 8 个微服务
└── 30-ingress.yaml                # Nginx Ingress 路由
```

---

## 九、网络策略（可选安全加固）

如需限制 Pod 间网络访问，可添加 NetworkPolicy:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: platform-network-policy
  namespace: data-platform
spec:
  podSelector: {}
  policyTypes:
    - Ingress
  ingress:
    # 网关可被 Ingress 命名空间访问
    - from:
        - namespaceSelector:
            matchLabels:
              name: ingress-nginx
      ports:
        - port: 8601
    # 微服务间可互相访问
    - from:
        - podSelector: {}
      ports:
        - port: 8800
        - port: 8801
        - port: 8802
        - port: 8803
        - port: 8804
        - port: 8805
        - port: 8603
    # 所有 Pod 可访问基础设施
    - from:
        - podSelector: {}
      ports:
        - port: 8848  # Nacos
        - port: 6379  # Redis
        - port: 9030  # Doris FE
        - port: 12345 # DolphinScheduler
        - port: 8585  # OpenMetadata
```

---

## 十、故障排查

### 10.1 Pod 无法启动

```bash
# 查看 Pod 事件
kubectl describe pod -n data-platform <pod-name>

# 常见原因:
# 1. 镜像拉取失败 → 检查镜像名和仓库认证
# 2. 资源不足 → 调整 ResourceQuota 或增加节点
# 3. PVC 未绑定 → 检查 StorageClass 是否正确
```

### 10.2 微服务无法连接 Nacos

```bash
# 进入 Pod 测试网络
kubectl exec -it -n data-platform <pod-name> -- sh

# 测试 Nacos 连通性
curl http://nacos:8848/nacos/v1/ns/instance/list?serviceName=data-gateway

# 如果失败:
# 1. 检查 Nacos 是否就绪: kubectl get pods -l app=nacos
# 2. 检查 Service: kubectl get svc nacos
# 3. 检查 DNS: nslookup nacos.data-platform.svc.cluster.local
```

### 10.3 Doris BE 未加入集群

```bash
# 进入 Doris FE
kubectl exec -it -n data-platform doris-fe-0 -- bash
mysql -h 127.0.0.1 -P 9030 -u root

# 检查 BE 状态
SHOW BACKENDS;

# 如果 BE 不在列表中，手动添加
ALTER SYSTEM ADD BACKEND "doris-be-0.doris-be.data-platform.svc.cluster.local:9050";

# 检查 BE 日志
kubectl logs -n data-platform doris-be-0 --tail=100
```

### 10.4 OpenMetadata 启动失败

```bash
# 检查 PostgreSQL 连接
kubectl exec -it -n data-platform <om-pod> -- sh
curl http://postgres-om:5432  # 应返回连接信息

# 检查 Elasticsearch
curl http://elasticsearch-om:9200/_cluster/health

# 查看 OM 启动日志
kubectl logs -n data-platform -l app=openmetadata --tail=200
```

---

## 十一、生产环境建议

### 11.1 高可用配置

| 服务 | 开发环境 | 生产环境 |
|------|---------|---------|
| Nacos | 1 副本 | 3 副本集群模式 |
| Doris FE | 1 副本 | 3 副本 (高可用) |
| Doris BE | 1 副本 | 3+ 副本 (数据冗余) |
| Redis | 1 副本 | 哨兵或集群模式 |
| PostgreSQL | 1 副本 | 主从复制 |
| Elasticsearch | 1 副本 | 3 节点集群 |
| data-gateway | 2 副本 | 3+ 副本 |
| 核心微服务 | 1 副本 | 2+ 副本 |

### 11.2 监控告警

```bash
# 安装 Prometheus + Grafana
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace

# 微服务暴露 metrics
# 在 application.yml 中添加:
# management.endpoints.web.exposure.include: health,info,prometheus
# management.metrics.export.prometheus.enabled: true
```

### 11.3 日志收集

```bash
# 使用 EFK (Elasticsearch + Fluentd + Kibana) 或 Loki
helm install loki grafana/loki-stack --namespace logging --create-namespace
```

### 11.4 备份策略

```bash
# Doris 数据备份: 通过 Doris Backup 功能
# PostgreSQL 备份: pg_dump
# K8s 资源备份: Velero
# PVC 快照: VolumeSnapshot
```

---

## 十二、清理

```bash
# 删除所有资源（保留 PV）
kubectl delete -f docker/k8s/

# 删除命名空间（级联删除所有资源）
kubectl delete namespace data-platform

# 清理 PV（如需要）
kubectl get pv | grep data-platform | awk '{print $1}' | xargs kubectl delete pv
```

