#!/usr/bin/env sh
set -eu

SCRIPT_PATH="${0:-}"
if [ -n "$SCRIPT_PATH" ] && [ -f "$SCRIPT_PATH" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$SCRIPT_PATH")" && pwd 2>/dev/null || pwd)"
else
  SCRIPT_DIR="$(pwd)"
fi

cd "$SCRIPT_DIR"

command -v java >/dev/null 2>&1 || {
  echo "Java 17 or newer is required." >&2
  exit 1
}
[ -f "$SCRIPT_DIR/backend/app.jar" ] || {
  echo "Missing backend/app.jar" >&2
  exit 1
}
[ -f "$SCRIPT_DIR/frontend/dist/index.html" ] || {
  echo "Missing frontend/dist/index.html" >&2
  exit 1
}

mkdir -p "$SCRIPT_DIR/config" "$SCRIPT_DIR/uploads"

export SERVER_PORT="${SERVER_PORT:-18080}"
export SERVER_ADDRESS="${SERVER_ADDRESS:-127.0.0.1}"
export BLOG_WEB_ROOT="${BLOG_WEB_ROOT:-file:$SCRIPT_DIR/frontend/dist/}"
export BLOG_UPLOAD_DIR="${BLOG_UPLOAD_DIR:-$SCRIPT_DIR/uploads}"
export BOKEXITONG_RUNTIME_CONFIG="${BOKEXITONG_RUNTIME_CONFIG:-$SCRIPT_DIR/config/bokexitong-runtime.properties}"
export BLOG_INSTALL_RESTART_AFTER_INSTALL="${BLOG_INSTALL_RESTART_AFTER_INSTALL:-true}"

if [ -f "$BOKEXITONG_RUNTIME_CONFIG" ]; then
  exec java -jar "$SCRIPT_DIR/backend/app.jar" --spring.profiles.active=prod --spring.config.additional-location="optional:file:$BOKEXITONG_RUNTIME_CONFIG" "$@"
fi

child_pid=""
stopping=0

stop_child() {
  stopping=1
  if [ -n "$child_pid" ]; then
    kill -TERM "$child_pid" 2>/dev/null || true
  fi
}

trap stop_child INT TERM

java -jar "$SCRIPT_DIR/backend/app.jar" --spring.profiles.active=prod "$@" &
child_pid=$!

set +e
wait "$child_pid"
exit_code=$?
set -e
child_pid=""

if [ "$stopping" -eq 1 ]; then
  exit "$exit_code"
fi

if [ -f "$BOKEXITONG_RUNTIME_CONFIG" ]; then
  echo "Installation configuration detected. Restarting Java with the runtime configuration..."
  exec java -jar "$SCRIPT_DIR/backend/app.jar" --spring.profiles.active=prod --spring.config.additional-location="optional:file:$BOKEXITONG_RUNTIME_CONFIG" "$@"
fi

exit "$exit_code"
