# Distributed Proxy Rate Limiter

A reverse proxy that enforces configurable, Redis-backed rate limits in front of an arbitrary
REST API. Policies are assigned to consumer groups per target resource or per resource group;
limits hold across any number of proxy instances. Built with Java 21, Spring Boot 3.5,
PostgreSQL, Redis, Bucket4j, Flyway and a Vaadin admin UI, following a hexagonal
(ports & adapters) architecture.

```
consumer ──Bearer JWT──▶ proxy (:8000) ──▶ target-api (:8001)
                           │   │
                    PostgreSQL  Redis (shared counters, cache invalidation)
```

## Prerequisites

* Docker Engine 24+ with Docker Compose v2 (Docker Desktop works too).
* Nothing else: **Java and Maven run only inside containers**; no JDK is needed on the host.
* Free ports `8000` (proxy), `8001` (target API), `5432`, `6379` - all configurable in `.env`.

## Configuration

All settings live in `.env` (created from `.env.example` by `run.sh` when missing) and are passed
to the containers by `src/main/docker/docker-compose.yml`.

| Variable | Default | Meaning |
|---|---|---|
| `PROXY_PORT` / `TARGET_API_PORT` | 8000 / 8001 | published ports |
| `PROXY_REPLICA_PORT` | 8002 | second proxy instance (`./run.sh --multi`) |
| `JWT_SECRET` | - | HS256 secret for consumer tokens (>= 32 bytes) |
| `JWT_DEFAULT_TTL_SECONDS` | 86400 | lifetime of tokens from `/admin/api/tokens` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | admin / ... | admin API (HTTP Basic) and UI (form login) |
| `LRU_CACHE_CAPACITY` / `LRU_CACHE_TTL` | 1000 / 3600 | MUC5 rule cache size and TTL (s) |
| `DEFAULT_POLICY_*` | default, moving-window, 60, minute | MUC3 default policy (first start only) |
| `TARGET_OPENAPI_PATH` | /v3/api-docs | OpenAPI document of the target |
| `RESOURCE_SYNC_ON_STARTUP` | true | import target endpoints when the proxy starts |
| `UPSTREAM_TIMEOUT_SECONDS` | 10 | connect / response timeout to the target |

## Start

```bash
./run.sh            # build images, start postgres, redis, target-api, proxy (waits until healthy)
./seed.sh           # demo consumers, policies, assignments + prints bearer tokens
./run.sh --multi    # additionally start a 2nd proxy instance on :8002 (same Redis/PostgreSQL)
./run.sh logs       # follow logs
./run.sh down       # stop (./run.sh down -v also drops the database volume)
```

The first build downloads Maven dependencies and Node.js (Vaadin production bundle); it takes a
few minutes. Later builds use the Docker build cache.

* Admin UI: <http://localhost:8000/ui/> (log in with `ADMIN_USERNAME` / `ADMIN_PASSWORD`)
* Admin API: `http://localhost:8000/admin/api/...` (HTTP Basic)
* Consumer traffic: any other path, e.g. `http://localhost:8000/orders/1` (Bearer token)
* Target API directly: <http://localhost:8001/swagger-ui.html>

## Tests

```bash
./test.sh           # all tests: unit + integration (Testcontainers) + target-api tests
./test.sh unit      # unit tests only (fast, no containers)
./test.sh perf      # Locust load test, headless 60 s, against the stack from ./run.sh + ./seed.sh
./test.sh perf-ui   # Locust web UI on http://localhost:8089
```

`test.sh` builds a Maven/JDK 21 container and mounts the host's Docker socket into it, so
Testcontainers can start PostgreSQL and Redis as sibling containers. Reports are copied to
`target/test-reports/`.

| Kind | Location | What it covers |
|---|---|---|
| Unit | `src/test/unit` | domain rules (path matching, conflict detection, rule selection), use cases with Mockito, adapters (OpenAPI parsing, JSON codes, header filtering), ArchUnit rules for the hexagon |
| Integration | `src/test/integration/<endpoint>` | every admin endpoint (CRUD, validation, filtering, paging, conflicts), proxy behaviour (CUC1), cache (MUC5), UI security (MUC6), DB triggers, concurrency and two-instance consistency for all three strategies |
| Performance | `src/test/performance/locustfile.py` | premium / regular / free / internal consumers hitting all endpoints; 429s are counted as expected outcomes |

Integration tests are named after the use cases (`test_muc1_*`, `test_cuc1_*`, ...), use
`db/seed/seed.sql` as their fixture and run inside a rolled-back transaction; tests that need
committed data (concurrency, triggers) clean up explicitly with `cleanup.sql`.

## Sample requests

See [`examples/sample-requests.http`](examples/sample-requests.http). A quick tour with curl:

```bash
source .env
TOKEN=$(curl -s -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"consumer_id":"free-user-1"}' localhost:8000/admin/api/tokens | sed -E 's/.*"access_token":"([^"]+)".*/\1/')

curl -i -H "Authorization: Bearer $TOKEN" localhost:8000/orders/1
# HTTP/1.1 200, X-RateLimit-Limit: 10, X-RateLimit-Remaining: 9, X-RateLimit-Strategy: fixed-window

for i in $(seq 1 10); do curl -s -o /dev/null -w '%{http_code} ' -H "Authorization: Bearer $TOKEN" localhost:8000/orders/1; done
# 200 ... 200 429
curl -s -H "Authorization: Bearer $TOKEN" localhost:8000/orders/1
# {"error":"You exceeded the request capacity for the current period","resource":"orders/1","https_status_code":429}

curl -s -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" localhost:8000/admin/api/policies/strategies
# {"supported_strategies":["moving-window","fixed-window","token-bucket"],"default_strategy":"moving-window",...}
```

## Repository layout

```
├── README.md, run.sh, test.sh, seed.sh, .env.example
├── docs/                      01-requirements, 02-architecture, 03-design-decisions, 04-ai-usage
├── db/migrations/             Flyway migrations (schema, default policy, integrity triggers)
├── db/seed/seed.sql           demo data, also the integration-test fixture
├── examples/                  sample-requests.http
├── src/main/java/...          proxy: domain / application / adapter / config
├── src/main/docker/           docker-compose.yml and Dockerfiles (proxy, target-api, tests)
├── src/test/unit|integration|performance
└── target-api/                stub target API (separate Spring Boot project)
```

Design documentation: [requirements](docs/01-requirements.md),
[architecture](docs/02-architecture.md), [design decisions](docs/03-design-decisions.md),
[AI usage](docs/04-ai-usage.md).
