#!/usr/bin/env bash
# Builds and starts the whole stack in Docker (no local Java/Maven needed).
#
#   ./run.sh            start postgres, redis, target-api and proxy
#   ./run.sh --multi    additionally start a second proxy instance (shared Redis/PostgreSQL)
#   ./run.sh logs       follow the logs
#   ./run.sh down       stop everything (add -v to also drop the database volume)
set -euo pipefail
cd "$(dirname "$0")"

[[ -f .env ]] || { cp .env.example .env; echo "Created .env from .env.example"; }
set -a; source .env; set +a
COMPOSE=(docker compose --env-file .env -f src/main/docker/docker-compose.yml)

case "${1:-up}" in
  logs) exec "${COMPOSE[@]}" logs -f ;;
  down) shift; exec "${COMPOSE[@]}" --profile multi --profile perf down "$@" ;;
  --multi) "${COMPOSE[@]}" --profile multi up -d --build --wait ;;
  up) "${COMPOSE[@]}" up -d --build --wait postgres redis target-api proxy ;;
  *) echo "usage: $0 [up|--multi|logs|down]"; exit 1 ;;
esac

cat <<INFO

Stack is up:
  Proxy (consumer traffic) http://localhost:${PROXY_PORT}/orders   (Bearer token required)
  Admin REST API           http://localhost:${PROXY_PORT}/admin/api  (Basic ${ADMIN_USERNAME})
  Admin UI                 http://localhost:${PROXY_PORT}/ui/
  Target API (direct)      http://localhost:${TARGET_API_PORT}/swagger-ui.html
Next: ./seed.sh   to load demo consumers, policies and assignments and print tokens.
INFO
