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
PACKAGE_DIR="$PROJECT_DIR/package"
BUILD_DIR="$PROJECT_DIR/.build-package/$APP_NAME"
PACKAGE_FILE="$PACKAGE_DIR/${APP_NAME}-linux.tar.gz"

info() { printf '\033[1;34m[INFO]\033[0m %s\n' "$*"; }
ok() { printf '\033[1;32m[ OK ]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*"; exit 1; }
has_cmd() { command -v "$1" >/dev/null 2>&1; }

copy_path() {
  src="$1"
  dest="$2"
  [ -e "$src" ] || return 0
  mkdir -p "$(dirname "$dest")"
  cp -R "$src" "$dest"
}

main() {
  cd "$PROJECT_DIR"
  has_cmd npm || fail "npm is required to build the frontend."
  has_cmd mvn || fail "mvn is required to build the backend."
  has_cmd tar || fail "tar is required to create the package."

  info "Building frontend..."
  (cd frontend && npm ci && npm run build)

  info "Building backend..."
  (cd backend && mvn -q -DskipTests package)

  rm -rf "$BUILD_DIR"
  mkdir -p "$BUILD_DIR/backend" "$BUILD_DIR/frontend" "$BUILD_DIR/sql" "$BUILD_DIR/scripts" "$PACKAGE_DIR"

  jar_file="$(find backend/target -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)"
  [ -n "$jar_file" ] || fail "Backend jar was not found in backend/target."
  cp "$jar_file" "$BUILD_DIR/backend/app.jar"
  cp backend/Dockerfile.runtime "$BUILD_DIR/backend/Dockerfile.runtime"
  cp -R frontend/dist "$BUILD_DIR/frontend/dist"
  cp frontend/nginx.conf "$BUILD_DIR/frontend/nginx.conf"
  cp frontend/Dockerfile.runtime "$BUILD_DIR/frontend/Dockerfile.runtime"
  cp -R sql/. "$BUILD_DIR/sql/"
  cp -R scripts/. "$BUILD_DIR/scripts/"
  copy_path docker-compose.runtime.yml "$BUILD_DIR/docker-compose.yml"
  copy_path docker-compose.yml "$BUILD_DIR/docker-compose.source.yml"
  copy_path .env.example "$BUILD_DIR/.env"
  copy_path .env.example "$BUILD_DIR/.env.example"
  copy_path README.md "$BUILD_DIR/README.md"
  copy_path deploy.sh "$BUILD_DIR/deploy.sh"
  copy_path install.sh "$BUILD_DIR/install.sh"
  copy_path uninstall.sh "$BUILD_DIR/uninstall.sh"
  copy_path shiye-bk "$BUILD_DIR/shiye-bk"
  copy_path docs "$BUILD_DIR/docs"

  chmod +x "$BUILD_DIR"/*.sh "$BUILD_DIR/scripts"/*.sh "$BUILD_DIR/shiye-bk" 2>/dev/null || true
  rm -f "$PACKAGE_FILE"
  tar -C "$(dirname "$BUILD_DIR")" -czf "$PACKAGE_FILE" "$APP_NAME"
  ok "Package created: $PACKAGE_FILE"
}

main "$@"
