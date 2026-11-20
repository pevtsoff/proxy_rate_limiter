#!/usr/bin/env bash
# Stops the whole stack started by ./run.sh (every profile: multi, perf).
#
#   ./stop.sh       stop and remove the containers (volumes are kept)
#   ./stop.sh -v    also drop the postgres/maven-repository volumes
set -euo pipefail
cd "$(dirname "$0")"

[[ -f .env ]] || { cp .env.example .env; echo "Created .env from .env.example"; }
set -a; source .env; set +a
COMPOSE=(docker compose --env-file .env -f src/main/docker/docker-compose.yml)

exec "${COMPOSE[@]}" --profile multi --profile perf down "$@"
