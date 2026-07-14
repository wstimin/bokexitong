#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd 2>/dev/null || pwd)"

if [ -f "$SCRIPT_DIR/scripts/uninstall.sh" ]; then
  exec bash "$SCRIPT_DIR/scripts/uninstall.sh" "$@"
fi

BRANCH="${BRANCH:-main}"
UNINSTALL_SCRIPT_URL="https://raw.githubusercontent.com/wstimin/bokexitong/${BRANCH}/scripts/uninstall.sh"
TMP_FILE="$(mktemp)"
trap 'rm -f "$TMP_FILE"' EXIT

curl -fsSL "$UNINSTALL_SCRIPT_URL" -o "$TMP_FILE"
exec bash "$TMP_FILE" "$@"
