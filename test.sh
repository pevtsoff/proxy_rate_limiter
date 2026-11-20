#!/usr/bin/env bash
# Runs the automated tests inside Docker (Maven + JDK 21 container; no local Java needed).
#
#   ./test.sh          unit + integration tests (Testcontainers: PostgreSQL, Redis) + target-api
#   ./test.sh unit     unit tests only (fast, no containers)
#   ./test.sh perf     Locust load test (headless, 60 s) against the stack started by ./run.sh
#   ./test.sh perf-ui  Locust web UI at http://localhost:${LOCUST_PORT}
#
# Reports are copied to ./target/test-reports.
set -euo pipefail
cd "$(dirname "$0")"

[[ -f .env ]] || cp .env.example .env
set -a; source .env; set +a
COMPOSE=(docker compose --env-file .env -f src/main/docker/docker-compose.yml)
CONTAINER=proxy-rate-limiter-tests

run_maven() {
  "${COMPOSE[@]}" --profile test build tests
  docker rm -f "${CONTAINER}" >/dev/null 2>&1 || true
  set +e
  "${COMPOSE[@]}" --profile test run --name "${CONTAINER}" tests "$@"
  local status=$?
  set -e
  mkdir -p target/test-reports
  for dir in target/surefire-reports target/failsafe-reports target-api/target/surefire-reports; do
    docker cp "${CONTAINER}:/workspace/${dir}" "target/test-reports/${dir//\//_}" \
      >/dev/null 2>&1 || true
  done
  docker rm -f "${CONTAINER}" >/dev/null 2>&1 || true
  echo "Test reports: target/test-reports"
  return "${status}"
}

case "${1:-all}" in
  all) run_maven ;;
  unit) run_maven mvn -B test ;;
  perf) "${COMPOSE[@]}" --profile perf run --rm locust \
          -f /mnt/locust/locustfile.py --host "http://proxy:${PROXY_PORT}" \
          --headless --users 50 --spawn-rate 10 --run-time 60s --only-summary ;;
  perf-ui) "${COMPOSE[@]}" --profile perf up -d locust
           echo "Locust UI: http://localhost:${LOCUST_PORT}" ;;
  *) echo "usage: $0 [all|unit|perf|perf-ui]"; exit 1 ;;
esac
