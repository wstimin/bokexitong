#!/usr/bin/env sh
set -eu

CONFIG_FILE="${BOKEXITONG_RUNTIME_CONFIG:-/app/config/bokexitong-runtime.properties}"

if [ -f "$CONFIG_FILE" ]; then
  unset SPRING_DATASOURCE_URL
  unset SPRING_DATASOURCE_USERNAME
  unset SPRING_DATASOURCE_PASSWORD
  unset BLOG_JWT_SECRET
  exec java -jar /app/blog-backend.jar --spring.profiles.active=prod --spring.config.additional-location="file:${CONFIG_FILE}" "$@"
fi

exec java -jar /app/blog-backend.jar --spring.profiles.active=prod "$@"
