#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then cp .env.example .env; fi
docker compose build proxy target-api
docker compose up -d postgres redis target-api
trap 'docker compose down' EXIT
docker compose run --rm --no-deps proxy uv run --no-sync pytest src/test
