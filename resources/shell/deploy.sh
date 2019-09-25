#!/bin/bash

# env
branch=${1:-master}

module=`pwd`
module=`echo ${module%_*}`
module=`echo ${module##*/}`

docker ps | grep ${module}:${branch}-latest | awk '{print $1}' | xargs docker kill || true
docker images | grep ${module}:${branch}-latest | awk '{print $1":"$2}' | xargs docker rmi -f || true
docker pull ${module}:${branch}-latest
docker run -d ${module}:${branch}-latest

# docker ps | grep node | awk '{print $1}' | xargs docker kill || true
# docker images | grep node | awk '{print $1":"$2}' | xargs docker rmi -f || true
# docker pull node || true
# docker run -d node || true