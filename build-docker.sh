#!/bin/bash

JAR_NAME=`ls build/libs | grep api-management- | grep -vE "asc|-plain"`
JAR_VERSION=${JAR_NAME%.*}
JAR_VERSION=${JAR_VERSION#*-}
JAR_VERSION=${JAR_VERSION#*-}
BUILD_TIME=$(date "+%Y%m%d%H%M%S")

echo ${BUILD_TIME} > .build_time

EXTRACTED=build/libs/extracted
mkdir -p ${EXTRACTED}

java -Djarmode=tools -jar build/libs/${JAR_NAME} extract --layers --launcher --destination ${EXTRACTED}
java tools/StripNative.java "${EXTRACTED}/dependencies/BOOT-INF/lib"

if [[ "${JAR_VERSION}" =~ ^.*SNAPSHOT$ ]]
then
  docker build --load -t ${DOCKER_HUB_URL}/api-management:snapshot-latest -t ${DOCKER_HUB_URL}/api-management:${JAR_VERSION} -t ${DOCKER_HUB_URL}/api-management:${JAR_VERSION}-${BUILD_TIME} .
else
  docker build --load -t ${DOCKER_HUB_URL}/api-management:latest -t ${DOCKER_HUB_URL}/api-management:${JAR_VERSION} -t ${DOCKER_HUB_URL}/api-management:${JAR_VERSION}-${BUILD_TIME} .
fi
