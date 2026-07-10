#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
OUTPUT_DIR="${ROOT_DIR}/output"

echo "==> SECP-backend 编译开始"
echo "    项目目录: ${BACKEND_DIR}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "错误: 未找到 mvn，请先安装 Maven 并配置 PATH"
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "错误: 未找到 java，请先安装 JDK 17+"
  exit 1
fi

JAVA_VERSION="$(java -version 2>&1 | head -n 1)"
echo "    Java: ${JAVA_VERSION}"

cd "${BACKEND_DIR}"
mvn clean package -DskipTests

mkdir -p "${OUTPUT_DIR}"
JAR_FILE="$(ls -1 target/smart-elderly-care-backend-*.jar | head -n 1)"
cp "${JAR_FILE}" "${OUTPUT_DIR}/"
cp -r src/main/resources/application.yml "${OUTPUT_DIR}/" 2>/dev/null || true

echo "==> 编译完成"
echo "    产物目录: ${OUTPUT_DIR}"
ls -lh "${OUTPUT_DIR}"
