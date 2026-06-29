#!/usr/bin/env bash
set -euo pipefail

APP_NAME="anime-blog"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"

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
  else
    OS_ID="unknown"
    OS_LIKE=""
  fi
}

install_base_tools() {
  detect_os
  if [[ "$OS_ID" =~ (ubuntu|debian) || "$OS_LIKE" =~ debian ]]; then
    sudo_cmd apt-get update
    sudo_cmd apt-get install -y curl ca-certificates gnupg lsb-release git
  elif [[ "$OS_ID" =~ (centos|rocky|almalinux|rhel|fedora) || "$OS_LIKE" =~ (rhel|fedora) ]]; then
    if has_cmd dnf; then
      sudo_cmd dnf install -y curl ca-certificates git yum-utils
    else
      sudo_cmd yum install -y curl ca-certificates git yum-utils
    fi
  else
    warn "Unknown Linux distribution. The script will try the official Docker installer."
  fi
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
    return
  fi

  MYSQL_PASSWORD_INPUT=""
  JWT_SECRET_INPUT=""

  if [ "${DEPLOY_AUTO_YES:-}" = "1" ] || [ ! -t 0 ]; then
    info "Creating .env file with generated secrets."
  else
    info "Creating .env file. Press Enter to use generated defaults."
    read -r -p "MySQL root password: " MYSQL_PASSWORD_INPUT || true
    read -r -p "JWT secret, at least 32 chars: " JWT_SECRET_INPUT || true
  fi

  MYSQL_PASSWORD="${MYSQL_PASSWORD_INPUT:-$(openssl rand -hex 12 2>/dev/null || date +%s%N)}"
  JWT_SECRET="${JWT_SECRET_INPUT:-$(openssl rand -hex 32 2>/dev/null || date +%s%N)-jwt-secret-change-me}"

  cat > "$ENV_FILE" <<EOF
MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD
BLOG_JWT_SECRET=$JWT_SECRET
EOF
  chmod 600 "$ENV_FILE" || true
  ok ".env created."
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

deploy() {
  cd "$PROJECT_DIR"
  info "Building and starting services..."
  compose_cmd up -d --build
  ok "Services started."
}

fix_seed_data() {
  cd "$PROJECT_DIR"
  [ -f sql/fix_mojibake_seed_data.sql ] || return 0

  info "Applying seed data text fix..."
  MYSQL_PASSWORD="root123456"
  if [ -f "$ENV_FILE" ]; then
    MYSQL_PASSWORD="$(grep -E '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | tail -n 1 | cut -d= -f2- || true)"
    MYSQL_PASSWORD="${MYSQL_PASSWORD:-root123456}"
  fi

  for _ in $(seq 1 30); do
    if compose_cmd exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_PASSWORD" >/dev/null 2>&1; then
      compose_cmd exec -T mysql mysql -uroot -p"$MYSQL_PASSWORD" personal_blog < sql/fix_mojibake_seed_data.sql
      ok "Seed data text fixed."
      return
    fi
    sleep 2
  done

  warn "MySQL was not ready; skip seed data fix. Run: docker compose exec -T mysql mysql -uroot -p personal_blog < sql/fix_mojibake_seed_data.sql"
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

  cat <<'EOF'

Deployment finished.

Open in browser:
  http://YOUR_SERVER_IP/

Default accounts:
  admin / 123456
  demo  / 123456

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
  fix_seed_data
  show_status
}

main "$@"
