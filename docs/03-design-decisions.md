# 03 - Design decisions and trade-offs

Each decision lists the chosen option, the alternatives considered and rejected, and the
trade-off accepted.

## D1. Standalone reverse proxy (architectural assumption N1)

* **Chosen:** a separate HTTP proxy service in front of the target API.
* **Rejected:** a Spring `HandlerInterceptor` / servlet filter library inside the target -
  only works for JVM/Spring targets; an API gateway product (Kong, Envoy, Spring Cloud Gateway) -
  hides the logic the assignment asks us to build and adds a heavy dependency.
* **Trade-off:** one extra network hop and the proxy must relay bodies and headers correctly
  (hop-by-hop headers are dropped, bodies are buffered - no streaming).

## D2. Hexagonal architecture with a framework-free core

* **Chosen:** `domain` and `application` contain no Spring/JPA/Redis types; ports are plain
  interfaces; beans are created in `config/UseCaseConfiguration` (composition root). Transactions
  use a `UnitOfWorkPort` (implemented with `TransactionTemplate`) instead of `@Transactional`,
  caching uses a decorator instead of `@Cacheable` on the service. ArchUnit enforces the rules.
* **Rejected:** classic layered Spring app with annotations on services - less code, but business
  rules become entangled with framework proxies and are harder to unit-test.
* **Trade-off:** more types (ports, mappers, entities separate from domain records) and explicit
  wiring. Accepted because the core rules (matching, conflict detection, rule selection) are the
  heart of the system and are now tested without Spring.

## D3. Rate-limiting algorithms: Lua sliding log + Bucket4j

* **Chosen:** strategy pattern (`RateLimitAlgorithm`) with three implementations:
  * `moving-window` (default) - exact sliding-window log in a Redis sorted set via one Lua
    script. Bucket4j cannot express an exact sliding window, and the task requires
    `moving-window` as default.
  * `fixed-window` - Bucket4j bandwidth with interval refill (full budget restored per period).
  * `token-bucket` - Bucket4j bandwidth with greedy (continuous) refill.
* **Rejected:** only Bucket4j (would have to approximate the moving window); sliding-window
  *counter* approximation (two fixed windows weighted) - cheaper but not exact; Redis `INCR` +
  `EXPIRE` written by hand for all strategies - re-implements what Bucket4j does well.
* **Trade-off:** the sliding log stores one entry per request in the window: memory is
  `O(limit)` per consumer and scope (e.g. 10 000/day for an internal service is ~10 000 small
  sorted-set entries). For very high limits a token bucket is the better choice; this is why the
  strategy is configurable per policy. Adding a strategy = one new class + one enum value.

## D4. Counter key design

`rl:{consumer_id}:{scope}:{policy_id-strategy-limit-period}`

* Scope makes resource-group assignments share one budget and keeps resource budgets separate.
* The policy fingerprint means an edited policy starts with fresh counters and never
  misinterprets the state of the previous definition (Bucket4j configuration replacement and
  sliding-log reinterpretation are avoided).
* **Trade-off:** editing a policy resets everyone's consumption for it (documented assumption
  A13); old keys expire on their own (TTL = window / refill time).

## D5. Fail closed when Redis is down

* **Chosen:** HTTP 503 with the uniform JSON error.
* **Rejected:** fail open (forward unmetered) - keeps the API available but removes protection
  exactly when the system is degraded; local in-memory fallback limiter - limits would be
  multiplied by the number of instances.
* **Trade-off:** Redis becomes a hard dependency of the data path. In production this calls for
  Redis HA; making the behaviour configurable is listed as a product question.

## D6. `consumer_groups_policies` with two foreign-key columns

* **Chosen:** `relationship_type` plus `resource_id` / `resource_group_id`, each a real foreign
  key, with a CHECK that exactly the matching one is set. The REST API still exposes a single
  `resource_id` field as specified.
* **Rejected:** one polymorphic `resource_id` column - PostgreSQL cannot enforce referential
  integrity for it, so deleting a resource could leave dangling assignments.
* **Trade-off:** the persistence adapter maps one domain field onto two columns.

## D7. Enforcing the overlap and "non-empty group" rules twice

* **Chosen:** the application checks the rules (readable 409 messages, row lock per consumer
  group against races) **and** the database enforces them (BEFORE triggers for overlaps, deferred
  constraint triggers for "group has at least one consumer").
* **Rejected:** application-only checks (bypassable by SQL, racy without locks); database-only
  (cryptic errors, business rules hidden in SQL).
* **Trade-off:** the rule exists in Java and in PL/pgSQL; both are covered by tests
  (`AssignmentConflictDetectorTest`, `PolicyAssignmentEndpointIT`, `DatabaseConstraintsIT`).

## D8. Rule cache: Caffeine per instance + Redis pub/sub invalidation

* **Chosen:** Spring Cache with Caffeine (`maximumSize`, `expireAfterWrite`, `recordStats` from
  `.env`), applied via the `CachingApplicableRuleResolver` decorator; any configuration change
  evicts all entries after commit and broadcasts the eviction to the other instances.
* **Rejected:** Redis as the cache store - an extra network round trip per request for data that
  rarely changes; precise per-key eviction - one policy change can affect an unbounded set of
  `(consumer, path)` keys, so computing them costs more than refilling the cache.
* **Trade-offs:** Caffeine uses W-TinyLFU, a frequency-aware refinement of LRU with better hit
  rates (still bounded by capacity as required). A small race remains: a lookup that started
  before the commit can store a stale entry after the eviction; it lives at most `LRU_CACHE_TTL`.
  Data changed outside the application (e.g. `seed.sh`) needs `DELETE /admin/api/cache`.

## D9. Authentication

* **Consumers:** stateless HS256 JWT (Spring Security resource server), `sub` = consumer id.
  Simple, no external IdP needed locally; switching to an IdP is a JWKS URL change.
* **Admins:** HTTP Basic for the REST API, form login for the Vaadin UI, one configured user.
* **Rejected:** API keys stored in the database - would need key management UI and hashing;
  OAuth2 with Keycloak - violates "no complicated multi-service deployment".

## D10. Synchronous Spring MVC on virtual threads

* **Chosen:** Spring MVC + JDK `HttpClient` (blocking) on virtual threads.
* **Rejected:** WebFlux / Spring Cloud Gateway - higher throughput ceiling but reactive code is
  harder to read, and JPA/Vaadin are blocking anyway.
* **Trade-off:** request and response bodies are buffered in memory.

## D11. Transport-neutral proxy model

`ProxiedRequest` / `UpstreamResponse` are domain records, so the use case can be tested without
servlets and the forwarder can be swapped (e.g. for a non-blocking client) without touching the
core.

## D12. Admin UI with Vaadin calling the same use cases

* **Chosen:** Vaadin Flow views that are thin inbound adapters over the same use-case ports as
  the REST controllers, so validation, conflict checks and cache eviction behave identically.
  A reusable `PagedGrid` maps the grid 1:1 to the paged use cases (filtering + pagination).
* **Rejected:** Jmix - a full platform with its own data layer that would bypass the hexagon;
  a separate SPA - more moving parts.
* **Trade-off:** the Docker build downloads Node.js once to compile the Vaadin bundle.

## D13. Repository layout

The proxy is the root Maven project (matching the suggested `src/`, `db/migrations/`
structure); the stub target API is a separate Maven project in `target-api/` because it is a
different deployable with different dependencies. Flyway migrations live in `/db/migrations`
and are packaged onto the default classpath location. Docker files live in `src/main/docker`.

## D14. Tests

* Unit tests (`src/test/unit`) for domain rules, use cases (Mockito) and adapter logic; ArchUnit
  for layering.
* Integration tests (`src/test/integration/<endpoint>`) against real PostgreSQL and Redis
  (Testcontainers) and an in-process stub target. `@Transactional` rollback isolates most tests;
  tests that must commit (concurrency, commit-time triggers) clean up with `cleanup.sql`.
  Redis and the cache are reset before every test because rollback does not cover them.
* `db/seed/seed.sql` is both the demo data and the integration-test fixture.
* Locust for load, treating 429 as an expected outcome.
* Rejected: H2/embedded Redis - behaviour (triggers, Lua, `TIME`) differs from production.

## D15. Java 21 / Spring Boot 3.5 / Vaadin 24

Spring Boot 3.5 is the line that Vaadin 24 supports; Vaadin 25 requires Spring Boot 4.
Testcontainers 2.x is used because Docker Engine 29 dropped API versions older than 1.44.
