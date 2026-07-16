#!/usr/bin/env bash
set -euo pipefail

APP_NAME="bokexitong"
LEGACY_APP_NAME="anime-blog"
DEFAULT_INSTALL_DIR="/opt/bokexitong"

SOURCE_PATH="${0:-}"
if [ "${BASH_SOURCE+x}" = "x" ] && [ "${#BASH_SOURCE[@]}" -gt 0 ]; then
  SOURCE_PATH="${BASH_SOURCE[0]}"
fi
if [ -n "$SOURCE_PATH" ] && [ -f "$SOURCE_PATH" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$SOURCE_PATH")" && pwd 2>/dev/null || pwd)"
else
  SCRIPT_DIR="$(pwd)"
fi
if [ -f "$SCRIPT_DIR/../docker-compose.yml" ]; then
  PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
else
  PROJECT_DIR="${INSTALL_DIR:-$DEFAULT_INSTALL_DIR}"
fi

PURGE=0
REMOVE_DOCKER=0
YES=0

info() { printf '\033[1;34m[INFO]\033[0m %s\n' "$*"; }
ok() { printf '\033[1;32m[ OK ]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*"; exit 1; }

usage() {
  cat <<EOF
Usage: bash uninstall.sh [options]

Options:
  --purge          Remove containers, project images, database/upload volumes, .env and project files.
  --remove-docker  Also uninstall Docker packages after project cleanup. Use only on a dedicated server.
  -y, --yes        Run without confirmation prompts.
  -h, --help       Show this help.

Examples:
  bash uninstall.sh
  bash uninstall.sh --purge
  bash uninstall.sh --purge --remove-docker -y
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --purge) PURGE=1 ;;
    --remove-docker) REMOVE_DOCKER=1 ;;
    -y|--yes) YES=1 ;;
    -h|--help) usage; exit 0 ;;
    *) fail "Unknown option: $1" ;;
  esac
  shift
done

has_cmd() { command -v "$1" >/dev/null 2>&1; }

sudo_cmd() {
  if [ "${EUID:-$(id -u)}" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
}

confirm() {
  message="$1"
  if [ "$YES" = "1" ]; then
    return 0
  fi
  answer=""
  if [ -r /dev/tty ]; then
    printf '%s [y/N]: ' "$message" > /dev/tty
    read -r answer < /dev/tty || true
  else
    printf '%s [y/N]: ' "$message"
    read -r answer || true
  fi
  case "$answer" in
    y|Y|yes|YES) return 0 ;;
    *) fail "Cancelled." ;;
  esac
}

compose_cmd() {
  if [ -f "$PROJECT_DIR/.env" ]; then
    sudo_cmd docker compose --env-file "$PROJECT_DIR/.env" -f "$PROJECT_DIR/docker-compose.yml" "$@"
  else
    sudo_cmd docker compose -f "$PROJECT_DIR/docker-compose.yml" "$@"
  fi
}

safe_project_dir() {
  if [ ! -e "$PROJECT_DIR" ]; then
    return 1
  fi
  resolved="$(cd "$PROJECT_DIR" && pwd -P)"
  case "$resolved" in
    /|/opt|/usr|/var|/home|/root|/tmp) return 1 ;;
  esac
  [ "$(basename "$resolved")" = "$APP_NAME" ] || return 1
}

remove_known_containers() {
  if ! has_cmd docker; then
    return
  fi
  for name in \
    bokexitong-mysql bokexitong-backend bokexitong-frontend \
    anime-blog-mysql anime-blog-backend anime-blog-frontend; do
    if sudo_cmd docker ps -a --format '{{.Names}}' | grep -Fxq "$name"; then
      sudo_cmd docker rm -f "$name" >/dev/null 2>&1 || true
      ok "Removed container: $name"
    fi
  done
}

remove_known_images() {
  if ! has_cmd docker; then
    return
  fi
  for image in \
    bokexitong-backend bokexitong-frontend \
    anime-blog-backend anime-blog-frontend; do
    if sudo_cmd docker image inspect "$image:latest" >/dev/null 2>&1; then
      sudo_cmd docker rmi -f "$image:latest" >/dev/null 2>&1 || true
      ok "Removed image: $image:latest"
    fi
  done
}

remove_known_volumes() {
  if ! has_cmd docker; then
    return
  fi
  project_base="$(basename "$PROJECT_DIR" 2>/dev/null || printf '%s' "$APP_NAME")"
  for volume in \
    "${project_base}_mysql-data" "${project_base}_upload-data" \
    bokexitong_mysql-data bokexitong_upload-data \
    anime-blog_mysql-data anime-blog_upload-data \
    anime_blog_mysql-data anime_blog_upload-data; do
    if sudo_cmd docker volume inspect "$volume" >/dev/null 2>&1; then
      sudo_cmd docker volume rm -f "$volume" >/dev/null 2>&1 || true
      ok "Removed volume: $volume"
    fi
  done
}

stop_project_services() {
  if ! has_cmd docker; then
    warn "Docker is not installed. Skipping container cleanup."
    return
  fi
  if [ -f "$PROJECT_DIR/docker-compose.yml" ]; then
    info "Stopping Docker Compose services in $PROJECT_DIR..."
    if [ "$PURGE" = "1" ]; then
      compose_cmd down -v --remove-orphans || true
    else
      compose_cmd down --remove-orphans || true
    fi
  else
    warn "docker-compose.yml not found in $PROJECT_DIR. Cleaning known containers by name."
  fi
  remove_known_containers
  remove_known_images
  if [ "$PURGE" = "1" ]; then
    remove_known_volumes
  fi
}

remove_project_files() {
  [ "$PURGE" = "1" ] || return
  if safe_project_dir; then
    resolved="$(cd "$PROJECT_DIR" && pwd -P)"
    info "Removing project directory: $resolved"
    sudo_cmd rm -rf "$resolved"
    ok "Project files removed."
  elif [ -e "$PROJECT_DIR" ]; then
    warn "Refusing to delete unsafe project directory: $PROJECT_DIR"
    warn "Remove it manually after checking the path."
  fi
}

remove_menu_command() {
  [ "$PURGE" = "1" ] || return
  if [ -f /usr/local/bin/shiye-bk ]; then
    sudo_cmd rm -f /usr/local/bin/shiye-bk
    ok "Removed command: shiye-bk"
  fi
}

remove_docker_packages() {
  [ "$REMOVE_DOCKER" = "1" ] || return
  if ! has_cmd docker; then
    ok "Docker is not installed."
    return
  fi
  confirm "This will uninstall Docker from the server. Continue?"
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    os_id="${ID:-unknown}"
    os_like="${ID_LIKE:-}"
  else
    os_id="unknown"
    os_like=""
  fi

  if [[ "$os_id" =~ (ubuntu|debian) || "$os_like" =~ debian ]]; then
    sudo_cmd apt-get remove -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin docker.io docker-compose || true
    sudo_cmd apt-get autoremove -y || true
  elif [[ "$os_id" =~ (centos|rocky|almalinux|rhel|fedora) || "$os_like" =~ (rhel|fedora) ]]; then
    if has_cmd dnf; then
      sudo_cmd dnf remove -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin docker docker-client docker-common || true
    else
      sudo_cmd yum remove -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin docker docker-client docker-common || true
    fi
  else
    warn "Unknown Linux distribution. Please uninstall Docker manually if needed."
    return
  fi
  ok "Docker packages removed."
}

main() {
  info "Starting uninstall for $APP_NAME."
  if [ "$PURGE" = "1" ]; then
    warn "Purge mode will delete database volume, uploaded files and project files."
    confirm "Continue with purge uninstall?"
  fi
  stop_project_services
  remove_project_files
  remove_menu_command
  remove_docker_packages
  ok "Uninstall finished."
}

main "$@"
