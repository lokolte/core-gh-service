#!/bin/bash
export JAVA_OPTS="-server \
          -Xms512m \
          -XX:MaxGCPauseMillis=500 \
          -XX:+UseStringDeduplication \
          -Xmx3072m \
          -XX:+UseG1GC \
          -XX:ConcGCThreads=4 \
          -XX:ParallelGCThreads=4 \
          -Dpidfile.path=/dev/null \
          -Dhttp.address=0.0.0.0 \
          -Dhttp.port=8080 \
          -Dlogger.url=file:///$APP_BASE/target/core-gh-service/conf/logback.xml \
          -Denvironment=${ENVIRONMENT_NAME}"

unzip -n /core/target/universal/core-gh-service.zip -d /core/target
/core/target/core-gh-service/bin/core-gh-service