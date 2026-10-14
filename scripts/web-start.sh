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
export BLOG_UPDATE_MODE="${BLOG_UPDATE_MODE:-web}"
export BLOG_UPDATE_REQUEST_PATH="${BLOG_UPDATE_REQUEST_PATH:-$SCRIPT_DIR/config/update-request.json}"
export BLOG_UPDATE_STATUS_PATH="${BLOG_UPDATE_STATUS_PATH:-$SCRIPT_DIR/config/update-status.json}"

child_pid=""
stopping=0

stop_child() {
  stopping=1
  if [ -n "$child_pid" ]; then
    kill -TERM "$child_pid" 2>/dev/null || true
  fi
}

trap stop_child INT TERM

while :; do
  if [ -f "$BOKEXITONG_RUNTIME_CONFIG" ]; then
    started_with_config=1
    java -jar "$SCRIPT_DIR/backend/app.jar" --spring.profiles.active=prod --spring.config.additional-location="optional:file:$BOKEXITONG_RUNTIME_CONFIG" "$@" &
  else
    started_with_config=0
    java -jar "$SCRIPT_DIR/backend/app.jar" --spring.profiles.active=prod "$@" &
  fi
  child_pid=$!
  set +e
  wait "$child_pid"
  exit_code=$?
  set -e
  child_pid=""

  [ "$stopping" -eq 0 ] || exit "$exit_code"
  if [ "$exit_code" -eq 42 ] && [ -f "$SCRIPT_DIR/scripts/update-agent.sh" ]; then
    echo "Update requested. Handing off to the update agent..."
    update_agent_copy="$(mktemp)"
    cp "$SCRIPT_DIR/scripts/update-agent.sh" "$update_agent_copy"
    BLOG_PROJECT_DIR="$SCRIPT_DIR" BLOG_UPDATE_STATE_DIR="$SCRIPT_DIR/config" \
      sh "$update_agent_copy" --once web || true
    rm -f "$update_agent_copy"
    continue
  fi
  if [ "$started_with_config" -eq 0 ] && [ -f "$BOKEXITONG_RUNTIME_CONFIG" ]; then
    echo "Runtime configuration detected. Restarting Java..."
    continue
  fi
  exit "$exit_code"
done
