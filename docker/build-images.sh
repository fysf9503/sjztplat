#!/bin/bash
set -e

REGISTRY=${REGISTRY:-"data-platform"}
VERSION=${VERSION:-"1.0.0"}
PUSH_REGISTRY=${PUSH_REGISTRY:-""}

echo "===== 构建微服务镜像 ====="
echo "Registry: $REGISTRY"
echo "Version:  $VERSION"

# Maven 打包
echo ">>> Maven 打包..."
mvn clean package -DskipTests -q

# 构建镜像
build_image() {
  local module=$1
  local port=$2
  local name=$3
  local image="$REGISTRY/$name:$VERSION"

  echo ">>> 构建 $name (port: $port)..."
  docker build -f docker/Dockerfile \
    -t "$image" \
    "$module"

  if [ -n "$PUSH_REGISTRY" ]; then
    echo ">>> 推送 $image..."
    docker tag "$image" "$PUSH_REGISTRY/$name:$VERSION"
    docker push "$PUSH_REGISTRY/$name:$VERSION"
  fi
}

build_image "data-gateway" 8601 "data-gateway"
build_image "data-auth" 8603 "data-auth"
build_image "data-modules/system-service" 8800 "system-service"
build_image "data-modules/data-quality-service" 8801 "data-quality-service"
build_image "data-modules/data-realtime-service" 8802 "data-realtime-service"
build_image "data-modules/data-integration-service" 8803 "data-integration-service"
build_image "data-modules/data-governance-service" 8804 "data-governance-service"
build_image "data-modules/data-lineage-service" 8805 "data-lineage-service"

echo ""
echo "===== 构建完成 ====="
echo "镜像列表:"
docker images | grep "$REGISTRY" | grep "$VERSION"
echo ""
echo "如需推送到私有仓库:"
echo "  export PUSH_REGISTRY=harbor.your-company.com"
echo "  ./docker/build-images.sh"
