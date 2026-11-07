# 03 — Design decisions and trade-offs

## D1. Standalone reverse proxy instead of an in-app middleware

**Chosen:** a separate proxy in front of the target API (architectural assumption N1).
**Why:** the target's technology is unknown; a proxy protects any HTTP API unchanged and
keeps limiting logic out of business services.
**Rejected:** an ASGI/Servlet middleware or SDK (one per language, redeploy every service);
an off-the-shelf gateway (Kong, Envoy) — less code but hides the engineering the task asks
for and adds operational weight.
**Trade-off:** one extra hop (latency, a component to operate) and buffered bodies.

## D2. `limits` for enforcement, slowapi for the management API

slowapi binds one strategy per `Limiter` and attaches limits to route functions with
decorators. The proxy has a single catch-all route and a strategy *per policy*, chosen at
runtime, and slowapi's storage is synchronous (it would block the event loop with Redis).
**Chosen:** call the async `limits` API — the engine slowapi is built on — directly, one
strategy instance per strategy name over one async Redis storage. Policy strategies are
the keys of `limits.strategies.STRATEGIES`, read programmatically. slowapi still protects
`/admin/*` with a shared per-IP limit through a router-level dependency.
**Rejected:** slowapi `Limiter` per strategy with dynamic limit callables (sync storage,
shared default key function, awkward with a catch-all route); a hand-written Lua limiter
(reinventing tested code).

## D3. Bucket scope

Counters are keyed by `(consumer_id, scope_key)`. The scope is the assignment id, so a
resource-group or all-resources assignment gives one shared quota per consumer for all its
resources, while a resource assignment is a quota for that endpoint only. The default
policy counts per resource, and unknown paths share one bucket.
**Trade-off:** changing a policy's numbers starts fresh counters (the `limits` key includes
the amount and window) — acceptable for an admin action, and it avoids applying a new
limit to an old window.

## D4. Resolution precedence resource › resource group › all › default

Most specific wins, which is what managers expect when they add an exception. Each rule is
a small matcher class used by `PolicySelector`, so a new kind of assignment is a new
matcher, not an edit of existing logic (open/closed principle).

## D5. Two typed FK columns instead of a polymorphic `resource_id`

The spec's single `resource_id` pointing to resources *or* resource groups cannot be a
foreign key. Two nullable FKs plus a CHECK keep referential integrity and cascading
deletes in PostgreSQL. **Trade-off:** one extra column, slight deviation from the spec.

## D6. Overlap rules in the service layer

"A resource must not be covered by both its own and its group's assignment" spans two
tables and changes when resources move between groups. A trigger could enforce it but
would hide business rules in SQL and be harder to test. **Chosen:**
`AssignmentConflictChecker`, used by the API, the Admin UI and resource moves.
**Trade-off:** two concurrent admin requests could each pass the check; acceptable for an
admin-only, low-frequency operation (a `SERIALIZABLE` transaction or advisory lock per
consumer group would close it).

## D7. Shared LRU cache in Redis, invalidated wholesale

**Chosen:** entries are Redis strings with TTL; a sorted set scored by last access orders
them for LRU eviction above `LRU_CACHE_CAPACITY`. Any committed admin change deletes the
whole cache (`SqlAlchemyUnitOfWork` commit listeners; Admin UI `after_model_*` hooks).
**Why:** correct across instances with no pub/sub; admin writes are rare, so dropping
everything is cheaper than computing which keys a change affects.
**Rejected:** per-process `functools.lru_cache` (stale on other instances, no TTL, no
invalidation); precise invalidation by key (complex: one policy change touches unknown
consumer × path combinations); write-through cache of whole tables.
**Trade-offs:** a burst of DB reads after each change; the eviction bookkeeping costs one
extra Redis round-trip per cache hit; a narrow race may re-cache a just-invalidated result
for up to the TTL. Mitigation if needed: a generation counter in the key, bumped atomically
on change.

## D8. Consumer group created with its first consumers

The schema in the brief makes group and consumer mutually required. **Chosen:** create a
group with `consumer_ids` (min 1) in one transaction; refuse to move or delete a group's
last consumer; deleting a group deletes its consumers. **Rejected:** a deferrable
constraint / trigger (DB-specific, invisible in the API), a `consumer_id` column on groups
(redundant and contradictory with one-to-many).

## D9. JWT bearer tokens with a shared secret

Simple to run locally and verifiable without a network call. **Trade-off:** no revocation;
a real deployment would verify the identity provider's RS256 tokens via JWKS — only
`JwtTokenCodec` would change (it implements the `TokenDecoder` port).

## D10. Unknown paths are forwarded and limited by default

The proxy should not need a resync to keep working when the target adds endpoints; the
default policy still applies, through a single shared bucket. **Alternative:** reject
unknown paths with 404 (stricter, but couples availability to resync discipline).

## D11. Ports, unit of work, composition root

Services depend on Protocols (`PolicyResolver`, `RateLimiter`, `UpstreamForwarder`,
`TokenDecoder`, `ApiSpecSource`, `PolicyCache`) and on a unit of work; concrete adapters
are wired only in `container_factory` and `dependencies`. This is what lets unit tests use
fakes and integration tests swap the HTTP client for an in-process target API.
**Trade-off:** more small files (one class per file, per the coding standards).

## D12. Tests against real PostgreSQL and Redis, rolled back per test

Integration tests run in Docker against the same engines as production, inside an outer
transaction rolled back after each test (`join_transaction_mode="create_savepoint"`).
The target API is called in-process through `httpx.ASGITransport`, so tests need no
running target container. **Rejected:** SQLite/fakeredis for integration (dialect
differences: partial indexes, FK enforcement, Lua scripts).

## D13. Other choices

* **Enums as VARCHAR + CHECK**: adding a strategy or method needs no `ALTER TYPE`.
* **Alembic** migrations run at proxy start; tests use `create_all` on a separate database.
* **uv** for dependency management; one image with `runtime` and `test` stages.
* **Several uvicorn workers** in compose to exercise the distributed behaviour locally.
* **Rate-limit headers** (`X-RateLimit-*`, `Retry-After`) on every proxied response.
* **Logging**: domain errors at WARNING with traceback, rate-limit rejections at INFO
  without traceback (they are normal traffic and would flood logs), unexpected errors at
  ERROR via `ExceptionLoggingMiddleware`.
