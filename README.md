# Distributed Rate Limiter

A configurable, distributed **proxy rate limiter** that sits in front of any REST API.
API managers define policies and assign them to consumer groups per resource, per
resource group or for all resources; consumers call the proxy exactly as they would call
the target API, with a bearer token.

```
client ──Bearer JWT──▶ proxy rate limiter ──▶ target API
                         │        │
                    PostgreSQL   Redis (counters + LRU policy cache)
```

* **Proxy rate limiter** (`src/main/proxy`): FastAPI, async SQLAlchemy 2, PostgreSQL,
  redis-py asyncio, `limits`/slowapi, sqladmin.
* **Target API** (`src/main/target_api`): a demo FastAPI app exposing `/orders`, `/users`,
  `/carts` (GET, POST, PUT, PATCH) that logs every request.

See [docs/](docs) for requirements, architecture, design decisions and AI usage.

## Prerequisites

* Docker with Compose v2 (`docker compose version`)
* Bash (Linux/macOS, or Git Bash/WSL on Windows)

Nothing else: Python, PostgreSQL and Redis all run in containers.
For working on the code outside Docker: Python 3.13 and [uv](https://docs.astral.sh/uv/).

## Configuration

All configuration lives in `.env` (a copy of [.env.example](.env.example); `run.sh`
creates it if missing). The most relevant settings:

| Variable | Default | Meaning |
|---|---|---|
| `LRU_CACHE_CAPACITY` / `LRU_CACHE_TTL` | 1000 / 3600 | policy cache size and TTL (s) |
| `DEFAULT_POLICY_*` | moving-window, 60/minute | default policy (MUC3) |
| `JWT_SECRET` | dev value | HS256 secret for bearer tokens |
| `PROXY_WORKERS` | 2 | uvicorn workers = proxy instances |
| `ADMIN_RATE_LIMIT` | 1000/minute | slowapi limit of the management API |

## Start

```bash
./run.sh
```

This builds the image, starts PostgreSQL, Redis, the target API and the proxy (running
Alembic migrations), then runs `./seed.sh`, which loads demo data and prints a bearer
token for every demo consumer.

| URL | What |
|---|---|
| http://localhost:8000/{path} | the proxy: any target API path, with a bearer token |
| http://localhost:8000/admin/docs | management API (Swagger UI) |
| http://localhost:8000/admin-ui | Admin UI (sqladmin) |
| http://localhost:8001/docs | target API, directly |

Stop with `docker compose --env-file .env -f src/main/docker/compose.yaml down`
(add `-v` only if you want to delete the database and Redis volumes).

## Tests

```bash
./test.sh                                   # unit + integration tests (pytest) in Docker
./test.sh src/test/unit                     # only unit tests
./test.sh -k cuc1                           # any pytest arguments
./test.sh perf                              # headless Locust load test (stack must run)
```

* `src/test/unit` — fast, isolated tests of domain objects, services and adapters.
* `src/test/integration/<endpoint>` — against real PostgreSQL and Redis, named after the
  use cases (`test_muc1_*`, `test_cuc1_*`, ...). Each test runs in a transaction that is
  rolled back afterwards; the `seeded` fixture loads the same seed as `./seed.sh`.
* `src/test/performance` — Locust users for the free, regular and premium tiers. Run
  `./test.sh perf` for a headless load test against a running stack. To use the Locust
  web UI, start the stack with:

  ```bash
  docker compose --env-file .env -f src/main/docker/compose.yaml --profile perf up locust
  ```

  Then open http://localhost:8089.

Without Docker (unit tests need no services):

```bash
uv sync
uv run pytest src/test/unit
```

## Sample requests

More in [examples/sample-requests.http](examples/sample-requests.http).

```bash
# token for a seeded consumer
TOKEN=$(curl -s -X POST localhost:8000/admin/tokens \
  -H 'Content-Type: application/json' -d '{"consumer_id": "free-user-1"}' | jq -r .access_token)

# through the proxy: free tier allows 10 requests/minute across all resources
for i in $(seq 1 11); do
  curl -s -o /dev/null -w "%{http_code}\n" localhost:8000/orders/1 -H "Authorization: Bearer $TOKEN"
done
```

The eleventh call returns:

```json
{"error": "You exceeded the request capacity for the current period", "resource": "orders/1", "https_status_code": 429}
```

with `Retry-After` and `X-RateLimit-Limit/Remaining/Reset` headers. Allowed responses carry
the `X-RateLimit-*` headers too.

Managing policies:

```bash
curl -X POST localhost:8000/admin/resources/sync           # import endpoints from swagger
curl -X POST localhost:8000/admin/policies -H 'Content-Type: application/json' \
  -d '{"name": "burst", "number_of_requests": 5, "time_period": "second"}'
curl -X POST localhost:8000/admin/consumer-groups-policies -H 'Content-Type: application/json' \
  -d '{"consumer_group_id": 1, "relationship_type": "resource", "policy_id": 8, "resource_id": 3}'
```

## Demo data (seed)

| Consumer group | Consumers | Assignments |
|---|---|---|
| free | free-user-1, free-user-2 | all resources: 10/minute (fixed window) |
| regular | regular-user-1, regular-user-2 | groups `orders`, `users`: 100/minute; `POST /carts`: 5/minute; other cart endpoints: default policy |
| premium | premium-user-1 | all resources: 500/minute; `GET /orders/{item_id}`: 3/second |
| internal | internal-service-1 | all resources: 10000/minute (sliding window counter) |

## Repository layout

```
├── run.sh, test.sh, seed.sh, .env(.example), pyproject.toml, alembic.ini
├── docs/                    01-requirements … 04-ai-usage
├── db/migrations/           Alembic (async)
├── examples/                sample-requests.http
└── src/
    ├── main/
    │   ├── docker/          Dockerfile, compose.yaml, postgres init
    │   ├── proxy/           domain · ports · services · repositories · infrastructure · api · admin_ui · seeding
    │   └── target_api/
    └── test/
        ├── unit/  integration/<endpoint>/  performance/
        └── support/         test doubles and helpers
```
