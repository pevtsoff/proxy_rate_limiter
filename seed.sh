#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
[[ -f .env ]] || cp .env.example .env
# Rebuild app images so the seed command uses the current source code.
docker compose up --build -d postgres redis target-api proxy
proxy_url="http://localhost:${PROXY_PORT:-8000}"
for attempt in {1..30}; do
  if curl -fsS "$proxy_url/health" >/dev/null 2>&1; then break; fi
  sleep 2
done
if ! curl -fsS "$proxy_url/health" >/dev/null; then
  echo "Proxy did not become healthy; check the Docker Compose logs." >&2
  exit 1
fi
echo "Syncing target API routes into the proxy..."
curl -fsS -X POST "$proxy_url/admin-api/resources/sync"
echo
docker compose exec -T proxy uv run --no-sync python -m app.seed
echo "Seeded 10 consumer groups and 20 consumers across free, regular, premium, and internal types."
echo "Created resource groups: orders, users, carts. Each has policies for all seeded consumer groups."
echo "Consumer IDs follow <group-name>-consumer-01 and <group-name>-consumer-02."
echo
echo "Try the proxy with these seeded consumers:"
echo "  curl -i '$proxy_url/orders/1' -H 'Authorization: Bearer premium-standard-consumer-01'"
echo "  curl -i '$proxy_url/users/1' -H 'Authorization: Bearer free-limited-consumer-01'"
echo "  curl -i -X POST '$proxy_url/carts' -H 'Authorization: Bearer regular-standard-consumer-01'"
echo "Admin UI: $proxy_url/admin"
