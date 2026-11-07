# Distributed rate limiter proxy

A local asynchronous FastAPI reverse proxy that applies per-consumer policies stored in PostgreSQL and enforces shared request counters in Redis. The included FastAPI target is a separate service used for demonstration. See [requirements](docs/01-requirements.md), [architecture](docs/02-architecture.md), [decisions](docs/03-design-decisions.md), and [AI usage](docs/04-ai-usage.md).

## Prerequisites and startup

Install Docker Engine and Docker Compose v2. `./run.sh` creates `.env` from `.env.example` on first run, builds the containers, and starts PostgreSQL, Redis, target API, and proxy. Stop with Ctrl+C. The proxy is at `http://localhost:8000`; target API is at `http://localhost:8001`; Admin UI is at `http://localhost:8000/admin`.

`./seed.sh` starts the stack, synchronizes target routes, and creates a sample policy. Use Admin UI to create a consumer group and consumer, then assign a policy to a resource or resource group. An incoming `Authorization: Bearer <consumer-id>` header identifies the consumer.

## Example

```sh
curl -X POST http://localhost:8000/admin-api/resources/sync
curl -i http://localhost:8000/orders/1 -H 'Authorization: Bearer demo-consumer'
```

Before configuring a consumer and assignment, requests with an unassigned known consumer use the default limit. Unknown consumers are treated as having the default policy in this demonstration.

## Tests

`./test.sh` builds the local images, starts PostgreSQL/Redis/target, and runs pytest in the proxy container. `locust -f src/test/performance/locustfile.py --host http://localhost:8000` runs the optional performance profile after seeding. Tests and dependencies run locally; no cloud credentials are needed.

Configuration is in `.env`; safe defaults are documented in `.env.example`. The default request limit is 100 per minute, LRU capacity is 1000, and TTL is 3600 seconds.
