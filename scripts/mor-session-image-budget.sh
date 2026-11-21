#!/usr/bin/env bash
# Mor session image-budget guard.
#
# The provider rejects a request when the image content sent for that request
# exceeds 30MB ("Downloaded image content cannot exceed 30MB"). OpenCode keeps
# older conversation stored in its DB but stops sending it after compaction.
# This guard measures only the images in the ACTIVE context (parts newer than
# the latest compaction), so it reports what will actually be sent.
#
# Usage:
#   scripts/mor-session-image-budget.sh
#
# Exit codes: 0 OK, 1 WARNING (compact soon), 2 CRITICAL (compact now),
#             3 could not determine session.
#
# Env: OPENCODE_DB, OPENCODE_SESSION_ID, MOR_IMAGE_BUDGET_BYTES (20MB),
#      MOR_IMAGE_CRITICAL_BYTES (28MB).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB="${OPENCODE_DB:-$HOME/.local/share/opencode/opencode.db}"
BUDGET="${MOR_IMAGE_BUDGET_BYTES:-20000000}"
CRITICAL="${MOR_IMAGE_CRITICAL_BYTES:-28000000}"

[[ -f "$DB" ]] || { echo "UNKNOWN: opencode DB not found: $DB"; exit 3; }

SESSION="${OPENCODE_SESSION_ID:-}"
if [[ -z "$SESSION" ]]; then
  PROJECT="$(sqlite3 "$DB" "select id from project where worktree='$ROOT' order by time_updated desc limit 1;" 2>/dev/null || true)"
  [[ -n "$PROJECT" ]] || { echo "UNKNOWN: no project row for $ROOT"; exit 3; }
  SESSION="$(sqlite3 "$DB" "select id from session where project_id='$PROJECT' order by time_updated desc limit 1;" 2>/dev/null || true)"
fi
[[ -n "$SESSION" ]] || { echo "UNKNOWN: no session for $ROOT"; exit 3; }

T="$(sqlite3 "$DB" "select coalesce(max(time_created),0) from part where session_id='$SESSION' and json_extract(data,'\$.type')='compaction';")"
read -r ACTIVE_COUNT ACTIVE_BYTES < <(sqlite3 -separator ' ' "$DB" \
  "select count(*), coalesce(sum(length(data)),0) from part where session_id='$SESSION' and time_created > $T and data like '%data:image%';")
read -r TOTAL_COUNT TOTAL_BYTES < <(sqlite3 -separator ' ' "$DB" \
  "select count(*), coalesce(sum(length(data)),0) from part where session_id='$SESSION' and data like '%data:image%';")

echo "session=$SESSION"
echo "latest_compaction_ms=$T"
echo "active_image_parts=$ACTIVE_COUNT"
echo "active_image_bytes=$ACTIVE_BYTES"
echo "session_total_image_parts=$TOTAL_COUNT"
echo "session_total_image_bytes=$TOTAL_BYTES"
echo "budget=$BUDGET critical=$CRITICAL"
echo "compact_command=opencode api post /api/session/$SESSION/compact --data '{}'"

if (( ACTIVE_BYTES >= CRITICAL )); then
  echo "CRITICAL: active image payload is near the provider's 30MB request limit. Compact now or start a fresh session before reading more images."
  exit 2
elif (( ACTIVE_BYTES >= BUDGET )); then
  echo "WARNING: active image payload is above the project budget. Compact soon or start a fresh session."
  exit 1
else
  echo "OK: active image payload is below the project budget."
  exit 0
fi
