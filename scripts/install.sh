#!/usr/bin/env bash
set -euo pipefail

APP_NAME="bokexitong"
REPO_URL="${REPO_URL:-https://github.com/wstimin/bokexitong.git}"
BRANCH="${BRANCH:-main}"
INSTALL_DIR="${INSTALL_DIR:-/opt/bokexitong}"
PACKAGE_URL="${PACKAGE_URL:-https://github.com/wstimin/bokexitong/releases/latest/download/bokexitong-linux.tar.gz}"

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

  fail "apt-get update failed. Please fix APT sources and run the installer again."
}

install_base_tools() {
  detect_os
  info "Installing required base tools..."
  if [[ "$OS_ID" =~ (ubuntu|debian) || "$OS_LIKE" =~ debian ]]; then
    apt_get_update
    sudo_cmd apt-get install -y git curl ca-certificates openssl tar
  elif [[ "$OS_ID" =~ (centos|rocky|almalinux|rhel|fedora) || "$OS_LIKE" =~ (rhel|fedora) ]]; then
    if has_cmd dnf; then
      sudo_cmd dnf install -y git curl ca-certificates openssl tar
    else
      sudo_cmd yum install -y git curl ca-certificates openssl tar
    fi
  else
    warn "Unknown Linux distribution. Please make sure git, curl and openssl are installed."
  fi
}

prepare_install_dir() {
  sudo_cmd mkdir -p "$(dirname "$INSTALL_DIR")"
  if [ ! -e "$INSTALL_DIR" ]; then
    sudo_cmd mkdir -p "$INSTALL_DIR"
    sudo_cmd chown "$(id -u):$(id -g)" "$INSTALL_DIR" 2>/dev/null || true
  elif [ ! -w "$INSTALL_DIR" ]; then
    sudo_cmd chown -R "$(id -u):$(id -g)" "$INSTALL_DIR" 2>/dev/null || true
  fi

  if [ ! -w "$INSTALL_DIR" ]; then
    fail "No write permission for $INSTALL_DIR. Run as root or set INSTALL_DIR to a writable path."
  fi
}

install_from_package() {
  prepare_install_dir
  if [ -e "$INSTALL_DIR" ] && [ "$(find "$INSTALL_DIR" -mindepth 1 -maxdepth 1 2>/dev/null | wc -l)" -gt 0 ]; then
    return 1
  fi

  tmp_file="$(mktemp)"
  tmp_dir="$(mktemp -d)"

  info "Trying build package: $PACKAGE_URL"
  if ! curl -fL --connect-timeout 10 --retry 2 "$PACKAGE_URL" -o "$tmp_file"; then
    rm -f "$tmp_file"
    rm -rf "$tmp_dir"
    warn "Build package is not available. Falling back to source deployment."
    return 1
  fi
  tar -xzf "$tmp_file" -C "$tmp_dir"
  package_root="$tmp_dir/$APP_NAME"
  [ -d "$package_root" ] || package_root="$(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  [ -n "$package_root" ] && [ -d "$package_root" ] || fail "Invalid build package."
  [ -f "$package_root/docker-compose.yml" ] || fail "Invalid build package: docker-compose.yml is missing."
  [ -f "$package_root/backend/app.jar" ] || fail "Invalid build package: backend/app.jar is missing."
  [ -d "$package_root/frontend/dist" ] || fail "Invalid build package: frontend/dist is missing."
  [ -f "$package_root/scripts/deploy.sh" ] || fail "Invalid build package: scripts/deploy.sh is missing."

  rm -rf "$INSTALL_DIR"
  sudo_cmd mkdir -p "$INSTALL_DIR"
  sudo_cmd chown "$(id -u):$(id -g)" "$INSTALL_DIR" 2>/dev/null || true
  cp -R "$package_root"/. "$INSTALL_DIR"/
  rm -f "$tmp_file"
  rm -rf "$tmp_dir"
  ok "Build package is ready: $INSTALL_DIR"
}

clone_or_update_repo() {
  prepare_install_dir

  if [ -d "$INSTALL_DIR/.git" ]; then
    info "Project already exists at $INSTALL_DIR. Pulling latest code..."
    cd "$INSTALL_DIR"
    git fetch origin "$BRANCH"
    git checkout "$BRANCH"
    git pull --ff-only origin "$BRANCH"
  elif [ -e "$INSTALL_DIR" ] && [ "$(find "$INSTALL_DIR" -mindepth 1 -maxdepth 1 2>/dev/null | wc -l)" -gt 0 ]; then
    fail "$INSTALL_DIR already exists and is not an empty Git repository. Please move it away or set INSTALL_DIR=/your/path."
  else
    info "Cloning project to $INSTALL_DIR..."
    git clone --branch "$BRANCH" "$REPO_URL" "$INSTALL_DIR"
  fi

  ok "Project code is ready: $INSTALL_DIR"
}

prepare_project_files() {
  if [ -f "$INSTALL_DIR/scripts/deploy.sh" ] && [ -f "$INSTALL_DIR/docker-compose.yml" ]; then
    ok "Project files already exist: $INSTALL_DIR"
    return
  fi
  if install_from_package; then
    return
  fi
  clone_or_update_repo
}

run_project_deploy() {
  cd "$INSTALL_DIR"
  chmod +x scripts/deploy.sh
  DEPLOY_AUTO_YES=1 bash scripts/deploy.sh
}

install_menu_command() {
  cd "$INSTALL_DIR"
  chmod +x scripts/menu.sh
  tmp_file="$(mktemp)"
  cat > "$tmp_file" <<EOF
#!/usr/bin/env bash
set -euo pipefail
INSTALL_DIR="$INSTALL_DIR"
exec bash "\$INSTALL_DIR/scripts/menu.sh" "\$@"
EOF
  chmod +x "$tmp_file"
  sudo_cmd mv "$tmp_file" /usr/local/bin/shiye-bk
  ok "Menu command installed: shiye-bk"
}

main() {
  info "Starting one-click pull and deployment for $APP_NAME."
  install_base_tools
  prepare_project_files
  run_project_deploy
  install_menu_command
}

main "$@"
