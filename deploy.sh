#!/usr/bin/env bash
set -euo pipefail

SOURCE_PATH="${0:-}"
if [ "${BASH_SOURCE+x}" = "x" ] && [ "${#BASH_SOURCE[@]}" -gt 0 ]; then
  SOURCE_PATH="${BASH_SOURCE[0]}"
fi
if [ -n "$SOURCE_PATH" ] && [ -f "$SOURCE_PATH" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$SOURCE_PATH")" && pwd 2>/dev/null || pwd)"
else
  SCRIPT_DIR="$(pwd)"
fi
exec bash "$SCRIPT_DIR/scripts/deploy.sh" "$@"
