#!/usr/bin/env bash
set -Eeuo pipefail

REPOSITORY="${REPOSITORY:-wstimin/bokexitong}"
TARGET_REF="${TARGET_REF:-latest}"
APP_DIR="${APP_DIR:-$(pwd)}"
BACKUP_ROOT="${BACKUP_ROOT:-}"
DB_CONTAINER="${DB_CONTAINER:-}"
SKIP_DB_BACKUP="${SKIP_DB_BACKUP:-0}"
BACKUP_UPLOADS="${BACKUP_UPLOADS:-0}"
BUILD_MODE="${BUILD_MODE:-auto}"

WORK_DIR=""
ROLLBACK_DIR=""
NEW_DIR=""
INSTALL_STARTED=0
INSTALL_FINISHED=0

info() { printf '\033[1;34m[INFO]\033[0m %s\n' "$*"; }
ok() { printf '\033[1;32m[ OK ]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; exit 1; }
has_cmd() { command -v "$1" >/dev/null 2>&1; }

usage() {
  cat <<'EOF'
安全更新 1Panel / 宝塔 Web 包，不覆盖数据库、config/ 和 uploads/。

用法：
  bash update-1panel.sh --app-dir /网站运行目录 [选项]

选项：
  --app-dir DIR          包含 web-start.sh 的运行目录（必填或通过 APP_DIR 提供）
  --ref REF              更新到指定标签，默认 latest
  --db-container NAME    MySQL 容器名称；脚本通常会根据运行配置自动识别
  --skip-db-backup       已在 1Panel 手工备份数据库时使用
  --backup-uploads       额外压缩备份 uploads/（上传文件较多时会很慢）
  --build-mode MODE      auto、docker 或 native，默认 auto
  -h, --help             显示帮助

示例：
  bash update-1panel.sh --app-dir /opt/1panel/apps/openresty/openresty/www/sites/blog.example.com/index
  SKIP_DB_BACKUP=1 bash update-1panel.sh --app-dir /path/to/index
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --app-dir) [ "$#" -ge 2 ] || fail "--app-dir 缺少目录"; APP_DIR="$2"; shift 2 ;;
    --ref) [ "$#" -ge 2 ] || fail "--ref 缺少版本"; TARGET_REF="$2"; shift 2 ;;
    --db-container) [ "$#" -ge 2 ] || fail "--db-container 缺少名称"; DB_CONTAINER="$2"; shift 2 ;;
    --skip-db-backup) SKIP_DB_BACKUP=1; shift ;;
    --backup-uploads) BACKUP_UPLOADS=1; shift ;;
    --build-mode) [ "$#" -ge 2 ] || fail "--build-mode 缺少模式"; BUILD_MODE="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) fail "未知参数：$1" ;;
  esac
done

validate_app_dir() {
  case "$APP_DIR" in
    /*) ;;
    *) fail "APP_DIR 必须是绝对路径" ;;
  esac
  APP_DIR="${APP_DIR%/}"
  [ -n "$APP_DIR" ] && [ "$APP_DIR" != "/" ] || fail "拒绝使用危险的运行目录"
  [ -d "$APP_DIR" ] || fail "运行目录不存在：$APP_DIR"
  [ -f "$APP_DIR/web-start.sh" ] || fail "运行目录中没有 web-start.sh"
  [ -f "$APP_DIR/backend/app.jar" ] || fail "运行目录中没有 backend/app.jar"
  [ -f "$APP_DIR/frontend/dist/index.html" ] || fail "运行目录中没有 frontend/dist/index.html"
  [ -d "$APP_DIR/config" ] || fail "运行目录中没有 config/"
  [ -d "$APP_DIR/uploads" ] || fail "运行目录中没有 uploads/"
  [ -w "$APP_DIR" ] || fail "运行目录不可写，请使用 root 或 sudo 执行"
}

cleanup() {
  exit_code=$?
  if [ "$INSTALL_STARTED" = "1" ] && [ "$INSTALL_FINISHED" != "1" ]; then
    warn "安装未完成，正在回滚程序文件……"
    if [ -n "$ROLLBACK_DIR" ] && [ -d "$ROLLBACK_DIR" ]; then
      if [ -f "$ROLLBACK_DIR/app.jar" ]; then
        rm -f "$APP_DIR/backend/app.jar"
        mv "$ROLLBACK_DIR/app.jar" "$APP_DIR/backend/app.jar"
      fi
      if [ -d "$ROLLBACK_DIR/dist" ]; then
        rm -rf "$APP_DIR/frontend/dist"
        mv "$ROLLBACK_DIR/dist" "$APP_DIR/frontend/dist"
      fi
      if [ -f "$ROLLBACK_DIR/web-start.sh" ]; then
        rm -f "$APP_DIR/web-start.sh"
        mv "$ROLLBACK_DIR/web-start.sh" "$APP_DIR/web-start.sh"
      fi
      if [ -f "$ROLLBACK_DIR/update-agent.sh" ]; then
        mkdir -p "$APP_DIR/scripts"
        rm -f "$APP_DIR/scripts/update-agent.sh"
        mv "$ROLLBACK_DIR/update-agent.sh" "$APP_DIR/scripts/update-agent.sh"
      elif [ -f "$ROLLBACK_DIR/no-update-agent" ]; then
        rm -f "$APP_DIR/scripts/update-agent.sh"
      fi
    fi
    warn "程序文件已回滚；数据库、config/ 和 uploads/ 从未被替换。"
  fi
  [ -n "$NEW_DIR" ] && [ -d "$NEW_DIR" ] && rm -rf "$NEW_DIR"
  [ -n "$ROLLBACK_DIR" ] && [ -d "$ROLLBACK_DIR" ] && rm -rf "$ROLLBACK_DIR"
  [ -n "$WORK_DIR" ] && [ -d "$WORK_DIR" ] && rm -rf "$WORK_DIR"
  exit "$exit_code"
}
trap cleanup EXIT INT TERM

resolve_latest_ref() {
  if [ "$TARGET_REF" != "latest" ]; then
    return
  fi
  info "查询最新正式版本……"
  TARGET_REF="$(curl -fsSL --connect-timeout 12 --retry 2 \
    "https://api.github.com/repos/$REPOSITORY/tags?per_page=100" \
    | sed -n 's/.*"name"[[:space:]]*:[[:space:]]*"\(v[0-9][0-9.]*\)".*/\1/p' \
    | sort -V | tail -n 1)"
  [ -n "$TARGET_REF" ] || fail "无法获取最新版本标签"
}

download_source() {
  case "$TARGET_REF" in
    *[!A-Za-z0-9._/-]*|*..*) fail "版本标识不安全：$TARGET_REF" ;;
  esac
  archive="$WORK_DIR/source.tar.gz"
  if printf '%s' "$TARGET_REF" | grep -Eq '^v[0-9]+\.[0-9]+\.[0-9]+$'; then
    source_url="https://github.com/$REPOSITORY/archive/refs/tags/$TARGET_REF.tar.gz"
  else
    source_url="https://github.com/$REPOSITORY/archive/refs/heads/$TARGET_REF.tar.gz"
  fi
  info "下载源码：$TARGET_REF"
  curl -fL --connect-timeout 15 --retry 3 -H 'Cache-Control: no-cache' "$source_url" -o "$archive"
  mkdir -p "$WORK_DIR/source"
  tar -xzf "$archive" -C "$WORK_DIR/source"
  SOURCE_DIR="$(find "$WORK_DIR/source" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  [ -n "$SOURCE_DIR" ] && [ -f "$SOURCE_DIR/pom.xml" ] || fail "下载的源码包格式不正确"
}

build_source() {
  mode="$BUILD_MODE"
  if [ "$mode" = "auto" ]; then
    if has_cmd docker; then mode="docker"; else mode="native"; fi
  fi

  case "$mode" in
    docker)
      has_cmd docker || fail "缺少 Docker，无法使用 docker 构建模式"
      info "使用临时 Node.js 容器构建前端……"
      docker run --rm --user "$(id -u):$(id -g)" \
        -e HOME=/tmp -v "$SOURCE_DIR:/workspace" -w /workspace \
        node:22-bookworm sh -c 'cd frontend && npm ci && npm test && npm run build && npm audit --omit=dev'
      info "使用临时 Maven 容器构建后端……"
      docker run --rm --user "$(id -u):$(id -g)" \
        -e HOME=/tmp -v "$SOURCE_DIR:/workspace" -w /workspace \
        maven:3.9.9-eclipse-temurin-17 mvn --batch-mode verify
      ;;
    native)
      has_cmd node && has_cmd npm && has_cmd mvn || fail "本机构建需要 Node.js、npm、Maven 和 Java 17；也可以安装 Docker 后重试"
      info "使用本机工具构建前端……"
      (cd "$SOURCE_DIR/frontend" && npm ci && npm test && npm run build && npm audit --omit=dev)
      info "使用本机工具构建后端……"
      (cd "$SOURCE_DIR" && mvn --batch-mode verify)
      ;;
    *) fail "BUILD_MODE 只能是 auto、docker 或 native" ;;
  esac

  JAR_FILE="$(find "$SOURCE_DIR/backend/target" -maxdepth 1 -type f -name 'blog-backend-*.jar' ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)"
  [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ] || fail "后端构建产物不存在"
  [ -f "$SOURCE_DIR/frontend/dist/index.html" ] || fail "前端构建产物不存在"
  [ -f "$SOURCE_DIR/scripts/web-start.sh" ] || fail "新版缺少 web-start.sh"
  [ -f "$SOURCE_DIR/scripts/update-agent.sh" ] || fail "新版缺少 update-agent.sh"
  VERSION="$(sed -n '/<artifactId>personal-blog-system<\/artifactId>/{n;s/.*<version>\([^<]*\)<\/version>.*/\1/p;q;}' "$SOURCE_DIR/pom.xml")"
  [ -n "$VERSION" ] || fail "无法读取新版本号"
  ok "源码构建完成：v$VERSION"
}

decode_property() {
  sed -e 's/\\:/:/g' -e 's/\\=/=/g' -e 's/\\#/#/g' -e 's/\\!/!/g' -e 's/\\ / /g' -e 's/\\\\/\\/g'
}

property_value() {
  key="$1"
  file="$2"
  awk -v prefix="$key=" 'index($0, prefix) == 1 { print substr($0, length(prefix) + 1); exit }' "$file" | decode_property
}

find_database_container() {
  [ -n "$DB_CONTAINER" ] && { printf '%s' "$DB_CONTAINER"; return; }
  has_cmd docker || return 1
  if docker inspect "$DB_HOST" >/dev/null 2>&1; then
    printf '%s' "$DB_HOST"
    return
  fi
  for container in $(docker ps --format '{{.Names}}'); do
    aliases="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{range .Aliases}}{{println .}}{{end}}{{end}}' "$container" 2>/dev/null || true)"
    if printf '%s\n' "$aliases" | grep -Fxiq "$DB_HOST"; then
      printf '%s' "$container"
      return
    fi
  done
  return 1
}

backup_database() {
  [ "$SKIP_DB_BACKUP" = "1" ] && { warn "已按要求跳过数据库备份；确认你已在 1Panel 手工备份数据库。"; return; }
  runtime="$APP_DIR/config/bokexitong-runtime.properties"
  [ -f "$runtime" ] || fail "缺少运行配置，无法自动备份数据库。请先在 1Panel 备份数据库，再加 --skip-db-backup 重试。"

  jdbc_url="$(property_value 'spring.datasource.url' "$runtime")"
  DB_USER="$(property_value 'spring.datasource.username' "$runtime")"
  DB_PASSWORD="$(property_value 'spring.datasource.password' "$runtime")"
  case "$jdbc_url" in jdbc:mysql://*) ;; *) fail "无法识别数据库连接地址" ;; esac
  jdbc_target="${jdbc_url#jdbc:mysql://}"
  DB_HOST_PORT="${jdbc_target%%/*}"
  DB_NAME_QUERY="${jdbc_target#*/}"
  DB_NAME="${DB_NAME_QUERY%%\?*}"
  DB_HOST="${DB_HOST_PORT%%:*}"
  DB_PORT="${DB_HOST_PORT##*:}"
  [ "$DB_PORT" != "$DB_HOST_PORT" ] || DB_PORT=3306
  printf '%s' "$DB_NAME" | grep -Eq '^[A-Za-z0-9_]+$' || fail "数据库名格式不安全"
  [ -n "$DB_USER" ] || fail "无法读取数据库用户名"

  dump_tmp="$BACKUP_DIR/database.sql.tmp"
  dump_file="$BACKUP_DIR/database.sql.gz"
  container="$(find_database_container || true)"
  if [ -n "$container" ]; then
    info "从 MySQL 容器 $container 备份数据库 $DB_NAME……"
    dump_tool="$(docker exec "$container" sh -c 'command -v mysqldump || command -v mariadb-dump' 2>/dev/null | head -n 1 || true)"
    [ -n "$dump_tool" ] || fail "数据库容器中没有 mysqldump。请在 1Panel 手工备份后加 --skip-db-backup 重试。"
    if ! docker exec -e MYSQL_PWD="$DB_PASSWORD" "$container" "$dump_tool" \
      --single-transaction --quick --triggers --no-tablespaces \
      --default-character-set=utf8mb4 -u "$DB_USER" "$DB_NAME" > "$dump_tmp"; then
      rm -f "$dump_tmp"
      fail "数据库备份失败；没有替换任何程序文件"
    fi
  elif has_cmd mysqldump; then
    info "通过宿主机 mysqldump 备份数据库 $DB_NAME……"
    if ! MYSQL_PWD="$DB_PASSWORD" mysqldump -h "$DB_HOST" -P "$DB_PORT" \
      --single-transaction --quick --triggers --no-tablespaces \
      --default-character-set=utf8mb4 -u "$DB_USER" "$DB_NAME" > "$dump_tmp"; then
      rm -f "$dump_tmp"
      fail "数据库备份失败；没有替换任何程序文件"
    fi
  else
    fail "无法自动定位 MySQL。请先在 1Panel 创建数据库备份，再加 --skip-db-backup 重试；也可通过 --db-container 指定容器。"
  fi
  gzip -9 "$dump_tmp"
  mv "$dump_tmp.gz" "$dump_file"
  [ -s "$dump_file" ] || fail "数据库备份文件为空"
  ok "数据库已备份：$dump_file"
}

backup_files() {
  timestamp="$(date +%Y%m%d-%H%M%S)"
  [ -n "$BACKUP_ROOT" ] || BACKUP_ROOT="$APP_DIR/backups"
  mkdir -p "$BACKUP_ROOT"
  BACKUP_DIR="$BACKUP_ROOT/update-$timestamp"
  mkdir -p "$BACKUP_DIR"
  paths=(backend frontend config web-start.sh)
  [ -d "$APP_DIR/scripts" ] && paths+=(scripts)
  info "备份当前程序和运行配置……"
  tar -C "$APP_DIR" -czf "$BACKUP_DIR/program-config.tar.gz" "${paths[@]}"
  if [ "$BACKUP_UPLOADS" = "1" ]; then
    info "备份 uploads/，请耐心等待……"
    tar -C "$APP_DIR" -czf "$BACKUP_DIR/uploads.tar.gz" uploads
  else
    printf 'uploads/ 未被更新脚本修改；如需额外压缩备份，使用 --backup-uploads。\n' > "$BACKUP_DIR/uploads-preserved.txt"
  fi
  backup_database
  ok "完整备份目录：$BACKUP_DIR"
}

install_release() {
  NEW_DIR="$APP_DIR/.bokexitong-new-$$"
  ROLLBACK_DIR="$APP_DIR/.bokexitong-rollback-$$"
  mkdir -p "$NEW_DIR/backend" "$NEW_DIR/frontend" "$NEW_DIR/scripts" "$ROLLBACK_DIR"
  cp "$JAR_FILE" "$NEW_DIR/backend/app.jar"
  cp -a "$SOURCE_DIR/frontend/dist" "$NEW_DIR/frontend/dist"
  cp "$SOURCE_DIR/scripts/web-start.sh" "$NEW_DIR/web-start.sh"
  cp "$SOURCE_DIR/scripts/update-agent.sh" "$NEW_DIR/scripts/update-agent.sh"
  chmod 755 "$NEW_DIR/web-start.sh" "$NEW_DIR/scripts/update-agent.sh"
  [ -s "$NEW_DIR/backend/app.jar" ] || fail "新 JAR 校验失败"
  [ -f "$NEW_DIR/frontend/dist/index.html" ] || fail "新前端校验失败"

  INSTALL_STARTED=1
  info "原子替换程序文件；config/、uploads/ 和数据库不会被修改……"
  mv "$APP_DIR/backend/app.jar" "$ROLLBACK_DIR/app.jar"
  mv "$APP_DIR/frontend/dist" "$ROLLBACK_DIR/dist"
  mv "$APP_DIR/web-start.sh" "$ROLLBACK_DIR/web-start.sh"
  if [ -f "$APP_DIR/scripts/update-agent.sh" ]; then
    mv "$APP_DIR/scripts/update-agent.sh" "$ROLLBACK_DIR/update-agent.sh"
  else
    : > "$ROLLBACK_DIR/no-update-agent"
  fi
  mv "$NEW_DIR/backend/app.jar" "$APP_DIR/backend/app.jar"
  mv "$NEW_DIR/frontend/dist" "$APP_DIR/frontend/dist"
  mv "$NEW_DIR/web-start.sh" "$APP_DIR/web-start.sh"
  mkdir -p "$APP_DIR/scripts"
  mv "$NEW_DIR/scripts/update-agent.sh" "$APP_DIR/scripts/update-agent.sh"
  chmod 755 "$APP_DIR/web-start.sh" "$APP_DIR/scripts/update-agent.sh"
  [ -f "$APP_DIR/config/bokexitong-runtime.properties" ] || fail "运行配置校验失败"
  [ -d "$APP_DIR/uploads" ] || fail "上传目录校验失败"
  INSTALL_FINISHED=1
  ok "程序文件已更新到 v$VERSION"
}

main() {
  validate_app_dir
  has_cmd curl || fail "缺少 curl"
  has_cmd tar || fail "缺少 tar"
  has_cmd gzip || fail "缺少 gzip"
  WORK_DIR="$(mktemp -d)"
  resolve_latest_ref
  download_source
  build_source
  backup_files
  install_release
  printf '\n'
  ok "更新完成，博文数据库、config/ 和 uploads/ 均已保留。"
  printf '备份位置：%s\n' "$BACKUP_DIR"
  printf '下一步：进入 1Panel -> 网站 -> 运行环境，重启 bokexitong。\n'
  printf '重启后访问 /api/portal/version，确认返回版本 v%s。\n' "$VERSION"
}

main
