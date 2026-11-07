#!/usr/bin/env bash
# Seed consumers, consumer groups, policies, resource groups and policy assignments
# (src/main/proxy/seeding/seed_plan.py) and print a bearer token per demo consumer.
# The integration tests load the same seed through the `seeded` fixture.
set -euo pipefail
cd "$(dirname "$0")"

COMPOSE=(docker compose --env-file .env -f src/main/docker/compose.yaml)
"${COMPOSE[@]}" exec -T proxy python -m proxy.seeding
