#!/bin/bash

# env
branch=${1:-master}

module=`pwd`
module=`echo ${module%_*}`
module=`echo ${module##*/}`

# echo "容器列表：" + docker ps
docker ps | grep ${module}:${branch}-latest | awk '{print \$1}' | xargs docker kill || true

# echo "镜像列表：" + docker images
docker images | grep ${module}:${branch}-latest | awk '{print \$1":"\$2}' | xargs docker rmi -f || true

# echo "拉取镜像："
docker pull ${module}:${branch}-latest

# echo "启动容器："
docker run -d ${module}:${branch}-latest