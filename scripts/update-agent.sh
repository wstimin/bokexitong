#!/usr/bin/env sh
set -eu

ACTION="${1:---watch}"
MODE="${2:-${BLOG_UPDATE_MODE:-docker}}"
PROJECT_DIR="${BLOG_PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
STATE_DIR="${BLOG_UPDATE_STATE_DIR:-$PROJECT_DIR/config}"
REQUEST_FILE="$STATE_DIR/update-request.json"
STATUS_FILE="$STATE_DIR/update-status.json"

json_value() {
  key="$1"
  sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" "$REQUEST_FILE" | head -n 1
}

write_status() {
  state="$1"
  message="$2"
  version="${3:-}"
  mkdir -p "$STATE_DIR"
  temp_file="$STATUS_FILE.tmp"
  printf '{"state":"%s","message":"%s","targetVersion":"%s","updatedAt":"%s"}\n' \
    "$state" "$message" "$version" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$temp_file"
  mv "$temp_file" "$STATUS_FILE"
}

verify_project_dir() {
  [ -n "$PROJECT_DIR" ] && [ "$PROJECT_DIR" != "/" ] || return 1
  [ -d "$PROJECT_DIR" ] || return 1
  if [ "$MODE" = "docker" ]; then
    [ -f "$PROJECT_DIR/docker-compose.yml" ] || return 1
  else
    [ -f "$PROJECT_DIR/web-start.sh" ] || return 1
  fi
}

install_once() {
  [ -f "$REQUEST_FILE" ] || return 0
  verify_project_dir || {
    write_status "FAILED" "更新目录校验失败"
    rm -f "$REQUEST_FILE"
    return 1
  }

  package_url="$(json_value packageUrl)"
  expected_sha="$(json_value sha256)"
  target_version="$(json_value toVersion)"
  case "$package_url" in
    https://*) ;;
    *) write_status "FAILED" "更新包地址无效" "$target_version"; rm -f "$REQUEST_FILE"; return 1 ;;
  esac
  if [ "${#expected_sha}" -ne 64 ]; then
    write_status "FAILED" "更新清单缺少有效校验值" "$target_version"
    rm -f "$REQUEST_FILE"
    return 1
  fi
  case "$expected_sha" in
    *[!0-9a-fA-F]*) write_status "FAILED" "更新清单校验值无效" "$target_version"; rm -f "$REQUEST_FILE"; return 1 ;;
  esac

  work_dir="$(mktemp -d)"
  package_file="$work_dir/update.tar.gz"
  trap 'rm -rf "$work_dir"' EXIT INT TERM
  write_status "DOWNLOADING" "正在下载更新包" "$target_version"
  curl -fL --connect-timeout 12 --retry 2 "$package_url" -o "$package_file" || {
    write_status "FAILED" "更新包下载失败" "$target_version"
    rm -f "$REQUEST_FILE"
    return 1
  }
  actual_sha="$(sha256sum "$package_file" | awk '{print $1}')"
  [ "$actual_sha" = "$expected_sha" ] || {
    write_status "FAILED" "更新包完整性校验失败" "$target_version"
    rm -f "$REQUEST_FILE"
    return 1
  }

  mkdir -p "$work_dir/unpacked"
  tar -xzf "$package_file" -C "$work_dir/unpacked"
  package_root="$(find "$work_dir/unpacked" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  [ -n "$package_root" ] || {
    write_status "FAILED" "更新包格式不正确" "$target_version"
    rm -f "$REQUEST_FILE"
    return 1
  }

  write_status "INSTALLING" "正在安装并重启服务" "$target_version"
  if [ "$MODE" = "docker" ]; then
    [ -f "$package_root/docker-compose.yml" ] && [ -f "$package_root/backend/app.jar" ] || return 1
    cp -R "$package_root"/. "$PROJECT_DIR"/
    rm -f "$REQUEST_FILE"
    cd "$PROJECT_DIR"
    docker compose up -d --build --remove-orphans
  else
    [ -f "$package_root/backend/app.jar" ] && [ -f "$package_root/web-start.sh" ] || return 1
    cp -R "$package_root"/. "$PROJECT_DIR"/
    rm -f "$REQUEST_FILE"
  fi
  write_status "RESTARTING" "新版本已安装，服务正在恢复" "$target_version"
}

mkdir -p "$STATE_DIR"
if [ "$ACTION" = "--once" ]; then
  install_once
  exit 0
fi

while :; do
  if [ -f "$REQUEST_FILE" ]; then
    install_once || true
  fi
  sleep 3
done
