#!/bin/bash

# env
branch=${1:-master}
registry="192.168.1.62:5000"
timestamp=`date +%Y%m%d%H%M%S`

# 检索出所有Dockerfile
Dockerfiles=`find . -name Dockerfile`
echo "检索到Dockerfile：" "${Dockerfiles}"

# 检索到变更的module
files=`git diff --name-only HEAD~ HEAD`
echo "git提交的文件：" "${files[@]}"

module=`pwd`
module=`echo ${module%_*}`
module=`echo ${module##*/}`

echo "构建镜像：$registry/$module:$branch-$timestamp"
docker build --build-arg ACTIVE=${branch} -t ${registry}/${module}:${branch}-${timestamp} .
echo "上传镜像（tiemstamp）：$registry/$module:$branch-$timestamp"
docker push ${registry}/${module}:${branch}-${timestamp}
echo "上传镜像（latest）：$registry/$module:$branch-latest"
docker tag ${registry}/${module}:${branch}-${timestamp} ${registry}/${module}:${branch}-latest
docker push ${registry}/${module}:${branch}-latest
echo "构建完成！"
