#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd 2>/dev/null || pwd)"

if [ -f "$SCRIPT_DIR/scripts/install.sh" ]; then
  exec bash "$SCRIPT_DIR/scripts/install.sh" "$@"
fi

BRANCH="${BRANCH:-main}"
INSTALL_SCRIPT_URL="https://raw.githubusercontent.com/wstimin/bokexitong/${BRANCH}/scripts/install.sh"
TMP_FILE="$(mktemp)"
trap 'rm -f "$TMP_FILE"' EXIT

curl -fsSL "$INSTALL_SCRIPT_URL" -o "$TMP_FILE"
exec bash "$TMP_FILE" "$@"
