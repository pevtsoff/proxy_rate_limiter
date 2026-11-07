#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
[[ -f .env ]] || cp .env.example .env
docker compose up -d postgres redis target-api proxy
for attempt in {1..30}; do
  if curl -fsS http://localhost:${PROXY_PORT:-8000}/health >/dev/null 2>&1; then break; fi
  sleep 2
done
curl -fsS -X POST http://localhost:${PROXY_PORT:-8000}/admin-api/resources/sync
docker compose exec -T proxy python -m app.seed
echo "Seeded 10 consumer groups and 20 consumers across free, regular, premium, and internal types."
echo "Each group has its own per-minute policy assigned to the orders, users, and carts resource groups."
echo "Consumer IDs follow <group-name>-consumer-01 and <group-name>-consumer-02."
echo "Admin UI: http://localhost:${PROXY_PORT:-8000}/admin"
