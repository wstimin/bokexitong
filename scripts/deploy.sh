#!/usr/bin/env bash
set -euo pipefail

APP_NAME="bokexitong"
SOURCE_PATH="${0:-}"
if [ "${BASH_SOURCE+x}" = "x" ] && [ "${#BASH_SOURCE[@]}" -gt 0 ]; then
  SOURCE_PATH="${BASH_SOURCE[0]}"
fi
if [ -n "$SOURCE_PATH" ] && [ -f "$SOURCE_PATH" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$SOURCE_PATH")" && pwd 2>/dev/null || pwd)"
else
  SCRIPT_DIR="$(pwd)"
fi
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
JWT_PLACEHOLDER_OLD="change-this-secret-to-a-long-random-value-for-production"
JWT_PLACEHOLDER_COMPOSE="please-change-this-secret-to-at-least-32-characters"
ADMIN_PASSWORD_PLACEHOLDER="please-change-admin-password"
MYSQL_PASSWORD_PLACEHOLDER="root123456"

info() { printf '\033[1;34m[INFO]\033[0m %s\n' "$*"; }
ok() { printf '\033[1;32m[ OK ]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*"; exit 1; }

has_cmd() { command -v "$1" >/dev/null 2>&1; }

sudo_cmd() {
  if [ "${EUID:-$(id -u)}" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
}

detect_os() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS_ID="${ID:-unknown}"
    OS_LIKE="${ID_LIKE:-}"
    OS_CODENAME="${VERSION_CODENAME:-${UBUNTU_CODENAME:-}}"
  else
    OS_ID="unknown"
    OS_LIKE=""
    OS_CODENAME=""
  fi
}

disable_stale_backports_source() {
  codename="${OS_CODENAME:-}"
  [ -n "$codename" ] || return 1
  pattern="${codename}-backports"
  changed=0

  for file in /etc/apt/sources.list /etc/apt/sources.list.d/*.list; do
    [ -f "$file" ] || continue
    if sudo_cmd grep -qE "^[[:space:]]*deb(-src)?[[:space:]].*[[:space:]]${pattern}([[:space:]]|$)" "$file"; then
      backup="${file}.bak.$(date +%Y%m%d%H%M%S)"
      sudo_cmd cp "$file" "$backup"
      sudo_cmd sed -i -E "/^[[:space:]]*deb(-src)?[[:space:]].*[[:space:]]${pattern}([[:space:]]|$)/ s/^/# disabled by bokexitong installer: /" "$file"
      warn "Disabled stale APT backports source in $file (backup: $backup)."
      changed=1
    fi
  done

  [ "$changed" = "1" ]
}

apt_get_update() {
  if sudo_cmd apt-get update; then
    return
  fi

  warn "apt-get update failed. Checking for stale backports source..."
  if disable_stale_backports_source; then
    sudo_cmd apt-get update
    return
  fi

  fail "apt-get update failed. Please fix APT sources and run the deploy script again."
}

install_base_tools() {
  detect_os
  if [[ "$OS_ID" =~ (ubuntu|debian) || "$OS_LIKE" =~ debian ]]; then
    apt_get_update
    sudo_cmd apt-get install -y curl ca-certificates gnupg lsb-release git openssl
  elif [[ "$OS_ID" =~ (centos|rocky|almalinux|rhel|fedora) || "$OS_LIKE" =~ (rhel|fedora) ]]; then
    if has_cmd dnf; then
      sudo_cmd dnf install -y curl ca-certificates git yum-utils openssl
    else
      sudo_cmd yum install -y curl ca-certificates git yum-utils openssl
    fi
  else
    warn "Unknown Linux distribution. The script will try the official Docker installer."
  fi
}

generate_secret_hex() {
  bytes="$1"
  secret=""
  if has_cmd openssl; then
    secret="$(openssl rand -hex "$bytes" 2>/dev/null || true)"
  fi

  if [ -n "$secret" ]; then
    printf '%s' "$secret"
    return
  fi

  seed="$(date +%s%N)-$$-${RANDOM:-0}-$APP_NAME-$bytes"
  if has_cmd sha256sum; then
    printf '%s' "$seed" | sha256sum | awk '{print $1}'
  else
    printf '%s%s%s' "$seed" "$seed" "$seed" | tr -cd 'A-Za-z0-9'
  fi
}

generate_mysql_password() {
  printf 'Db-%s' "$(generate_secret_hex 18)"
}

generate_jwt_secret() {
  generate_secret_hex 32
}

generate_admin_password() {
  printf 'Admin-%s' "$(generate_secret_hex 18)"
}

install_docker() {
  if has_cmd docker && sudo_cmd docker compose version >/dev/null 2>&1; then
    ok "Docker and Docker Compose are already installed."
    return
  fi

  info "Installing Docker and Docker Compose plugin..."
  install_base_tools
  curl -fsSL https://get.docker.com | sudo_cmd sh
  sudo_cmd systemctl enable docker
  sudo_cmd systemctl start docker

  if ! sudo_cmd docker compose version >/dev/null 2>&1; then
    fail "Docker Compose plugin is not available after installation. Please install docker-compose-plugin manually."
  fi
  ok "Docker installed."
}

create_env_file() {
  if [ -f "$ENV_FILE" ]; then
    ok ".env already exists."
    ensure_env_defaults
    validate_env_file
    return
  fi

  MYSQL_PASSWORD_INPUT=""
  JWT_SECRET_INPUT=""
  ADMIN_PASSWORD_INPUT=""

  if [ "${DEPLOY_AUTO_YES:-}" = "1" ] || [ ! -t 0 ]; then
    info "Creating .env file with generated secrets."
  else
    info "Creating .env file. Press Enter to use generated defaults."
    read -r -p "MySQL root password: " MYSQL_PASSWORD_INPUT || true
    read -r -p "JWT secret, at least 32 chars: " JWT_SECRET_INPUT || true
    read -r -p "Initial admin password, at least 8 chars: " ADMIN_PASSWORD_INPUT || true
  fi

  MYSQL_PASSWORD="${MYSQL_PASSWORD_INPUT:-$(generate_mysql_password)}"
  JWT_SECRET="${JWT_SECRET_INPUT:-$(generate_jwt_secret)}"
  ADMIN_PASSWORD="${ADMIN_PASSWORD_INPUT:-$(generate_admin_password)}"

  [ "${#JWT_SECRET}" -ge 32 ] || fail "JWT secret must be at least 32 characters."
  [ "${#ADMIN_PASSWORD}" -ge 8 ] || fail "Initial admin password must be at least 8 characters."

  cat > "$ENV_FILE" <<EOF
MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD
BLOG_JWT_SECRET=$JWT_SECRET
BLOG_ADMIN_INITIAL_PASSWORD=$ADMIN_PASSWORD
BLOG_MAIL_ENABLED=false
BLOG_MAIL_FROM_NAME=博客系统
SPRING_MAIL_HOST=
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_STARTTLS_ENABLE=true
SPRING_MAIL_SSL_ENABLE=false
EOF
  chmod 600 "$ENV_FILE" || true
  validate_env_file
  ok ".env created."
}

append_env_if_missing() {
  key="$1"
  value="$2"
  if ! grep -qE "^${key}=" "$ENV_FILE"; then
    printf '%s=%s\n' "$key" "$value" >> "$ENV_FILE"
  fi
}

set_env_value() {
  key="$1"
  value="$2"
  tmp_file="${ENV_FILE}.tmp.$$"
  awk -v key="$key" -v value="$value" '
    BEGIN { replaced = 0 }
    $0 ~ "^" key "=" { print key "=" value; replaced = 1; next }
    { print }
    END { if (!replaced) print key "=" value }
  ' "$ENV_FILE" > "$tmp_file"
  mv "$tmp_file" "$ENV_FILE"
}

ensure_env_defaults() {
  append_env_if_missing "MYSQL_ROOT_PASSWORD" "$(generate_mysql_password)"
  append_env_if_missing "BLOG_JWT_SECRET" "$(generate_jwt_secret)"
  append_env_if_missing "BLOG_ADMIN_INITIAL_PASSWORD" "$(generate_admin_password)"
  append_env_if_missing "BLOG_MAIL_ENABLED" "false"
  append_env_if_missing "BLOG_MAIL_FROM_NAME" "博客系统"
  append_env_if_missing "SPRING_MAIL_HOST" ""
  append_env_if_missing "SPRING_MAIL_PORT" "587"
  append_env_if_missing "SPRING_MAIL_USERNAME" ""
  append_env_if_missing "SPRING_MAIL_PASSWORD" ""
  append_env_if_missing "SPRING_MAIL_SMTP_AUTH" "true"
  append_env_if_missing "SPRING_MAIL_STARTTLS_ENABLE" "true"
  append_env_if_missing "SPRING_MAIL_SSL_ENABLE" "false"

  if [ "$(env_value BLOG_JWT_SECRET)" = "$JWT_PLACEHOLDER_OLD" ] || [ "$(env_value BLOG_JWT_SECRET)" = "$JWT_PLACEHOLDER_COMPOSE" ]; then
    warn "BLOG_JWT_SECRET is still a placeholder. Replacing it with a generated secret."
    set_env_value "BLOG_JWT_SECRET" "$(generate_jwt_secret)"
  fi

  if [ "$(env_value BLOG_ADMIN_INITIAL_PASSWORD)" = "$ADMIN_PASSWORD_PLACEHOLDER" ]; then
    warn "BLOG_ADMIN_INITIAL_PASSWORD is still a placeholder. Replacing it with a generated password."
    set_env_value "BLOG_ADMIN_INITIAL_PASSWORD" "$(generate_admin_password)"
  fi

  chmod 600 "$ENV_FILE" || true
}

env_value() {
  key="$1"
  if [ ! -f "$ENV_FILE" ]; then
    return 0
  fi
  grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d= -f2-
}

validate_env_file() {
  mysql_password="$(env_value MYSQL_ROOT_PASSWORD)"
  jwt_secret="$(env_value BLOG_JWT_SECRET)"
  admin_password="$(env_value BLOG_ADMIN_INITIAL_PASSWORD)"
  mail_enabled="$(env_value BLOG_MAIL_ENABLED)"

  [ -n "$mysql_password" ] || fail "MYSQL_ROOT_PASSWORD is required in .env."
  [ "$mysql_password" != "$MYSQL_PASSWORD_PLACEHOLDER" ] || fail "MYSQL_ROOT_PASSWORD is still the example value. Set a real strong password in .env."
  case "$mysql_password" in
    *请改成*) fail "MYSQL_ROOT_PASSWORD is still a placeholder. Set a real strong password in .env." ;;
  esac

  [ -n "$jwt_secret" ] || fail "BLOG_JWT_SECRET is required in .env."
  [ "${#jwt_secret}" -ge 32 ] || fail "BLOG_JWT_SECRET must be at least 32 characters."
  [ "$jwt_secret" != "$JWT_PLACEHOLDER_OLD" ] || fail "BLOG_JWT_SECRET is still a placeholder."
  [ "$jwt_secret" != "$JWT_PLACEHOLDER_COMPOSE" ] || fail "BLOG_JWT_SECRET is still a placeholder."
  case "$jwt_secret" in
    *请改成*) fail "BLOG_JWT_SECRET is still a placeholder." ;;
  esac

  [ -n "$admin_password" ] || fail "BLOG_ADMIN_INITIAL_PASSWORD is required in .env."
  [ "${#admin_password}" -ge 8 ] || fail "BLOG_ADMIN_INITIAL_PASSWORD must be at least 8 characters."
  [ "$admin_password" != "$ADMIN_PASSWORD_PLACEHOLDER" ] || fail "BLOG_ADMIN_INITIAL_PASSWORD is still a placeholder."
  case "$admin_password" in
    *请改成*) fail "BLOG_ADMIN_INITIAL_PASSWORD is still a placeholder." ;;
  esac

  if [ "$mail_enabled" = "true" ]; then
    [ -n "$(env_value SPRING_MAIL_HOST)" ] || fail "SPRING_MAIL_HOST is required when BLOG_MAIL_ENABLED=true."
    [ -n "$(env_value SPRING_MAIL_USERNAME)" ] || fail "SPRING_MAIL_USERNAME is required when BLOG_MAIL_ENABLED=true."
    [ -n "$(env_value SPRING_MAIL_PASSWORD)" ] || fail "SPRING_MAIL_PASSWORD is required when BLOG_MAIL_ENABLED=true."
  fi
}

check_project_files() {
  cd "$PROJECT_DIR"
  [ -f docker-compose.yml ] || fail "docker-compose.yml not found. Run this script inside the project package."
  [ -f sql/personal_blog.sql ] || fail "sql/personal_blog.sql not found."
  [ -f backend/Dockerfile ] || fail "backend/Dockerfile not found."
  [ -f frontend/Dockerfile ] || fail "frontend/Dockerfile not found."
}

compose_cmd() {
  sudo_cmd docker compose "$@"
}

wait_for_mysql() {
  cd "$PROJECT_DIR"
  info "Waiting for MySQL to be ready..."
  for _ in $(seq 1 60); do
    if compose_cmd exec -T mysql sh -c 'mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot -p"$MYSQL_ROOT_PASSWORD" personal_blog -e "SELECT 1"' >/dev/null 2>&1; then
      ok "MySQL is ready."
      return
    fi
    sleep 2
  done
  fail "MySQL did not become ready in time. Check logs with: docker compose logs -f mysql"
}

run_sql_file() {
  sql_file="$1"
  [ -f "$sql_file" ] || return 0
  info "Applying database upgrade: $(basename "$sql_file")"
  for attempt in $(seq 1 5); do
    if compose_cmd exec -T mysql sh -c 'mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot -p"$MYSQL_ROOT_PASSWORD" personal_blog' < "$sql_file"; then
      return
    fi
    [ "$attempt" -lt 5 ] || break
    warn "Database upgrade failed, retrying ($attempt/5)..."
    sleep 3
  done
  fail "Failed to apply database upgrade: $(basename "$sql_file")"
}

run_database_upgrades() {
  cd "$PROJECT_DIR"
  if ! compgen -G "$PROJECT_DIR/sql/upgrade_*.sql" >/dev/null; then
    ok "No database upgrade scripts found."
    return
  fi

  run_sql_file "$PROJECT_DIR/sql/upgrade_site_setting.sql"
  for upgrade_file in "$PROJECT_DIR"/sql/upgrade_*.sql; do
    [ "$(basename "$upgrade_file")" = "upgrade_site_setting.sql" ] && continue
    run_sql_file "$upgrade_file"
  done
  ok "Database upgrades applied."
}

deploy() {
  cd "$PROJECT_DIR"
  info "Building and starting database..."
  compose_cmd up -d --build mysql
  wait_for_mysql
  run_database_upgrades

  info "Building and starting application services..."
  compose_cmd up -d --build backend frontend
  ok "Services started."
}

show_status() {
  cd "$PROJECT_DIR"
  info "Current service status:"
  compose_cmd ps

  info "Waiting for frontend to respond..."
  for _ in $(seq 1 30); do
    if curl -fsS http://127.0.0.1/ >/dev/null 2>&1; then
      ok "Frontend is available: http://127.0.0.1/"
      break
    fi
    sleep 2
  done

  info "Waiting for backend API to respond..."
  for _ in $(seq 1 30); do
    if curl -fsS http://127.0.0.1/api/portal/home >/dev/null 2>&1; then
      ok "Backend API is available: http://127.0.0.1/api/portal/home"
      break
    fi
    sleep 2
  done

  cat <<EOF

Deployment finished.

Open in browser:
  http://YOUR_SERVER_IP/

Initial admin account:
  username: admin
  password: $(env_value BLOG_ADMIN_INITIAL_PASSWORD)

Email verification:
  Registration and password reset require real SMTP settings.
  Edit .env and set BLOG_MAIL_ENABLED=true plus SPRING_MAIL_* values, then run: docker compose up -d

This is a clean installation. Create categories, tags, images and articles in the admin panel.

Useful commands:
  docker compose ps
  docker compose logs -f backend
  docker compose logs -f frontend
  docker compose logs -f mysql
  docker compose restart
  docker compose down

EOF
}

main() {
  info "Starting one-click deployment for $APP_NAME."
  check_project_files
  install_docker
  create_env_file
  deploy
  show_status
}

main "$@"
