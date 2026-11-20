#!/usr/bin/env bash
# CUC1.7: loads demo consumer groups, consumers, policies and assignments into the running
# stack (idempotent), evicts the rule cache and prints bearer tokens for the demo consumers.
# The same db/seed/seed.sql is the fixture of the integration tests.
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a
COMPOSE=(docker compose --env-file .env -f src/main/docker/docker-compose.yml)

echo "Seeding database ${POSTGRES_DB} ..."
"${COMPOSE[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 --single-transaction --quiet \
  -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" < db/seed/seed.sql

admin_call() {
  "${COMPOSE[@]}" exec -T proxy curl -fsS -u "${ADMIN_USERNAME}:${ADMIN_PASSWORD}" \
    -H 'Content-Type: application/json' "$@"
}

echo "Evicting the applicable-rule cache ..."
admin_call -X DELETE "http://localhost:${PROXY_PORT}/admin/api/cache"

echo "Bearer tokens (valid ${JWT_DEFAULT_TTL_SECONDS}s):"
for consumer in premium-user-1 regular-user-1 free-user-1 internal-service-1; do
  token=$(admin_call -X POST "http://localhost:${PROXY_PORT}/admin/api/tokens" \
    -d "{\"consumer_id\": \"${consumer}\"}" | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
  printf '  %-20s %s\n' "${consumer}" "${token}"
done

cat <<INFO

Try it:
  curl -i -H "Authorization: Bearer <free-user-1 token>" http://localhost:${PROXY_PORT}/orders/1
  (free tier: 10 reads/minute on /orders -> the 11th request returns HTTP 429)
INFO
