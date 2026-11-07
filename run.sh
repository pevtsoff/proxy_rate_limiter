#!/usr/bin/env bash
# Start PostgreSQL, Redis, the target API and the proxy rate limiter, then seed demo data.
# Usage: ./run.sh            (SKIP_SEED=1 ./run.sh to start without demo data)
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || cp .env.example .env
COMPOSE=(docker compose --env-file .env -f src/main/docker/compose.yaml)

"${COMPOSE[@]}" up --build --detach --wait proxy --force-recreate

if [ "${SKIP_SEED:-0}" != "1" ]; then
  ./seed.sh
fi

cat <<EOF

Proxy rate limiter : http://localhost:${PROXY_PORT:-8000}   (e.g. GET /orders/1 with a bearer token)
Management API docs: http://localhost:${PROXY_PORT:-8000}/admin/docs
Admin UI           : http://localhost:${PROXY_PORT:-8000}/admin-ui/
Target API (direct): http://localhost:${TARGET_API_PORT:-8001}/docs

Logs: docker compose --env-file .env -f src/main/docker/compose.yaml logs -f proxy target-api
Stop: docker compose --env-file .env -f src/main/docker/compose.yaml down

Manual policy/resource checks (demo data from ./seed.sh):
  curl -s http://localhost:${PROXY_PORT:-8000}/admin/resources | jq
  curl -s http://localhost:${PROXY_PORT:-8000}/admin/policies | jq

Get demo bearer tokens:
  curl -s -X POST http://localhost:${PROXY_PORT:-8000}/admin/tokens \\
    -H 'Content-Type: application/json' -d '{"consumer_id":"free-user-1"}' | jq
  curl -s -X POST http://localhost:${PROXY_PORT:-8000}/admin/tokens \\
    -H 'Content-Type: application/json' -d '{"consumer_id":"regular-user-1"}' | jq

Try the seeded policies through the proxy (set TOKEN to a token above):
  curl -i http://localhost:${PROXY_PORT:-8000}/orders/1 -H "Authorization: Bearer \$TOKEN"
  curl -i -X POST http://localhost:${PROXY_PORT:-8000}/carts \\
    -H 'Content-Type: application/json' -H "Authorization: Bearer \$TOKEN" -d '{}'
EOF
