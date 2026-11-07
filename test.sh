#!/usr/bin/env bash
# Run the automated tests inside docker compose (real PostgreSQL and Redis).
# Usage:
#   ./test.sh                         all unit + integration tests
#   ./test.sh src/test/unit -k cache  any pytest arguments
#   ./test.sh perf                    headless locust run against the running stack
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || cp .env.example .env
COMPOSE=(docker compose --env-file .env -f src/main/docker/compose.yaml)

if [ "${1:-}" = "perf" ]; then
  shift
  "${COMPOSE[@]}" --profile perf run --rm --service-ports locust \
    locust -f src/test/performance/locustfile.py --host http://proxy:8000 \
    --headless --users "${USERS:-30}" --spawn-rate "${SPAWN_RATE:-10}" \
    --run-time "${RUN_TIME:-1m}" "$@"
  exit 0
fi

"${COMPOSE[@]}" --profile test build tests
"${COMPOSE[@]}" up --detach --wait postgres redis
"${COMPOSE[@]}" --profile test run --rm tests pytest "$@"
