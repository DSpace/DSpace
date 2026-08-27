#!/usr/bin/env bash
# tests/wait-for-stack.sh
# Wait for all core DSpace services to be reachable before running tests.
#
# Usage:
#   ./tests/wait-for-stack.sh [max_seconds]
#
# Environment variables (all optional, defaults shown):
#   BACKEND_URL   http://localhost:8080
#   SOLR_URL      http://localhost:8983
#   FRONTEND_URL  http://localhost:4000
#   MAX_WAIT      300

set -euo pipefail

MAX_WAIT="${1:-${MAX_WAIT:-300}}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
SOLR_URL="${SOLR_URL:-http://localhost:8983}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4000}"

YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Waiting for DSpace stack (max ${MAX_WAIT}s)...${NC}"
start=$(date +%s)

wait_for() {
  local name="$1"
  local url="$2"
  printf "  %-20s" "$name"
  until curl -sf --max-time 5 "$url" > /dev/null 2>&1; do
    elapsed=$(( $(date +%s) - start ))
    if [ "$elapsed" -ge "$MAX_WAIT" ]; then
      echo -e " ${RED}TIMEOUT${NC} after ${elapsed}s waiting for $url"
      exit 1
    fi
    printf "."
    sleep 5
  done
  elapsed=$(( $(date +%s) - start ))
  echo -e " ${GREEN}ready${NC} (${elapsed}s)"
}

wait_for "backend"   "$BACKEND_URL/server/"
wait_for "solr"      "$SOLR_URL/solr/admin/info/system"
wait_for "frontend"  "$FRONTEND_URL/"

echo -e "${GREEN}All services ready.${NC}"

