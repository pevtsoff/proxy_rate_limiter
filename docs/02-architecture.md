# 02 - Architecture and design

## High-level component architecture

Two containers run application code (plus PostgreSQL and Redis):

* **target-api** - a stub Spring Boot REST API (`/orders`, `/users`, `/carts`; GET, POST, PUT,
  PATCH) that logs every request and publishes OpenAPI at `/v3/api-docs`.
* **proxy** - the Proxy Rate Limiter. Any number of instances can run side by side
  (`./run.sh --multi` starts two); they share PostgreSQL (configuration) and Redis (counters,
  cache invalidation).

```mermaid
flowchart LR
    C[Consumers<br/>mobile / web / services] -- Bearer JWT --> P1[proxy #1]
    C -- Bearer JWT --> P2[proxy #2]
    A[API manager] -- Basic / form login --> P1
    P1 & P2 -- forward allowed requests --> T[target-api]
    P1 & P2 -- config, JPA --> DB[(PostgreSQL)]
    P1 & P2 -- counters, Lua / Bucket4j CAS<br/>pub/sub invalidation --> R[(Redis)]
    P1 -- OpenAPI sync --> T
```

### Hexagonal structure of the proxy

```mermaid
flowchart TB
    subgraph in[Inbound adapters - adapter.in]
        REST[web.admin<br/>REST controllers]
        PROXY[web.proxy<br/>ProxyController]
        UI[ui<br/>Vaadin views]
        SEC[web.security / web.error]
    end
    subgraph core[Application core - framework free]
        PIN[[ports.in: use cases]]
        SVC[application.service]
        DOM[domain: model + services<br/>ResourcePathMatcher, AssignmentSelector,<br/>AssignmentConflictDetector]
        POUT[[ports.out]]
    end
    subgraph out[Outbound adapters - adapter.out]
        JPA[persistence<br/>JPA + Flyway]
        RL[ratelimit<br/>Bucket4j + Lua]
        HTTP[http<br/>forwarder, OpenAPI]
        CACHE[cache<br/>Caffeine decorator,<br/>Redis pub/sub]
        JWT[security<br/>JWT issuer]
    end
    REST & PROXY & UI --> PIN
    PIN --- SVC --> DOM
    SVC --> POUT
    POUT -.implemented by.- JPA & RL & HTTP & CACHE & JWT
    CFG[config: composition root] -.wires.- core
```

* **domain** - immutable records and pure services; depends only on the JDK.
* **application** - use-case interfaces (`port.in`), required interfaces (`port.out`) and their
  orchestration (`service`); depends only on the domain. Transactions go through
  `UnitOfWorkPort`, cache invalidation through `RuleCacheInvalidationPort`.
* **adapters** - Spring MVC, Vaadin, Spring Security, JPA, Redis, JDK HttpClient.
* **config** - the composition root that instantiates the core with `@Bean` methods.

These rules are enforced by `HexagonalArchitectureTest` (ArchUnit).

## Request execution flow (CUC1)

```mermaid
sequenceDiagram
    participant C as Consumer
    participant S as Security filter (JWT)
    participant PC as ProxyController
    participant PR as ProxyRequestService
    participant RR as CachingApplicableRuleResolver
    participant DB as PostgreSQL
    participant RL as RedisRateLimiterAdapter
    participant R as Redis
    participant T as target-api

    C->>S: GET /orders/1  Authorization: Bearer ...
    S->>S: verify HS256 signature + exp, principal = sub (consumer_id)
    S->>PC: authenticated request
    PC->>PR: ProxiedRequest(consumer, GET, /orders/1, headers, body)
    PR->>RR: resolve(consumer, GET, /orders/1)
    alt cache miss
        RR->>DB: consumer -> group, resources, assignments, policy
        DB-->>RR: ApplicableRule(policy, scope)
    end
    RR-->>PR: ApplicableRule (cached)
    PR->>RL: tryConsume("rl:{consumer}:{scope}:{policy}", policy)
    RL->>R: atomic Lua script / Bucket4j CAS
    R-->>RL: allowed, remaining / retry-after
    alt allowed
        PR->>T: forward (X-Consumer-Id, X-Forwarded-*)
        T-->>PC: response
        PC-->>C: target response + X-RateLimit-* headers
    else rejected
        PC-->>C: 429 {"error", "resource", "https_status_code"} + Retry-After
    end
```

Rule resolution: consumer -> consumer group -> best matching resource (OpenAPI template
matching; the most literal segments win) -> resource-level assignment, else resource-group
assignment, else the default policy. The **scope** decides which requests share a counter:
`resource:{id}`, `resource-group:{id}`, `default:resource:{id}` or `default:unmatched`.

## Database schema

```mermaid
erDiagram
    policies {
        bigint id PK
        varchar name UK
        varchar rate_limiting_strategy "moving-window | fixed-window | token-bucket"
        int number_of_requests "> 0"
        varchar time_period "second..week"
        boolean is_default "at most one TRUE"
        bigint version
    }
    resource_groups { bigint id PK
        varchar name UK }
    resources {
        bigint id PK
        varchar method "GET..DELETE"
        varchar resource_path
        varchar io_type "read | write"
        bigint resource_group_id FK "nullable, ON DELETE SET NULL"
    }
    consumer_groups { bigint id PK
        varchar group_name UK "premium | regular | free | internal" }
    consumers {
        bigint id PK
        varchar consumer_id UK
        bigint consumer_group_id FK "NOT NULL"
    }
    consumer_groups_policies {
        bigint id PK
        bigint consumer_group_id FK
        varchar relationship_type "resource | resource_group"
        bigint policy_id FK
        bigint resource_id FK "set iff type = resource"
        bigint resource_group_id FK "set iff type = resource_group"
    }
    resource_groups ||--o{ resources : groups
    consumer_groups ||--|{ consumers : "has >= 1"
    consumer_groups ||--o{ consumer_groups_policies : has
    policies ||--o{ consumer_groups_policies : "assigned by"
    resources ||--o{ consumer_groups_policies : targets
    resource_groups ||--o{ consumer_groups_policies : targets
```

Constraints (`db/migrations`):

* `UNIQUE(method, resource_path)`, unique names, enum CHECKs, `number_of_requests > 0`.
* Partial unique index: a single default policy.
* `cgp_exactly_one_target` CHECK and partial unique indexes `(consumer_group_id, resource_id)`
  / `(consumer_group_id, resource_group_id)`: one assignment per target and group.
* Trigger `prevent_overlapping_assignment` and `prevent_overlap_on_regroup`: a resource may not be
  assigned directly while its resource group is assigned for the same consumer group.
* Deferred constraint triggers: a consumer group must contain at least one consumer at commit.
* V2 inserts the default policy from `.env` values (Flyway placeholders).

## API contracts

All JSON uses `snake_case`; enum values use their external codes. Admin endpoints require HTTP
Basic (`ADMIN_USERNAME`/`ADMIN_PASSWORD`). Lists accept `page` (0-based) and `size` (1..500) and
return `{content, page, size, total_elements, total_pages}`.

| Method & path | Purpose |
|---|---|
| `GET/POST /admin/api/policies`, `GET/PUT/DELETE /admin/api/policies/{id}` | MUC4 CRUD; filters `name`, `strategy` |
| `GET /admin/api/policies/strategies` | supported strategies, default strategy, time periods |
| `GET /admin/api/policies/default` | MUC3 default policy |
| `GET/POST /admin/api/consumer-groups`, `GET/PUT/DELETE .../{id}`, `GET .../names` | MUC1.1; POST body `{group_name, consumer_ids[]}` |
| `GET/POST /admin/api/consumers`, `GET/PUT/DELETE .../{id}` | MUC1.2; filters `consumer_id`, `consumer_group_id` |
| `GET/POST /admin/api/resource-groups`, `GET/PUT/DELETE .../{id}` | resource groups |
| `GET/POST /admin/api/resources`, `GET/PUT/DELETE .../{id}` | resources; filters `path`, `method`, `io_type`, `resource_group_id` |
| `POST /admin/api/resources/sync` | MUC1.4 import from OpenAPI -> `{created[], unchanged, stale[]}` |
| `GET/POST /admin/api/consumer-groups-policies`, `GET/PUT/DELETE .../{id}` | MUC1.5 / MUC2; body `{consumer_group_id, relationship_type, policy_id, resource_id}`; filters `consumer_group_id`, `relationship_type`, `policy_id` |
| `POST /admin/api/tokens` | dev token `{consumer_id}` -> `{access_token, token_type, expires_at}` |
| `DELETE /admin/api/cache` | evict the rule cache on all instances |
| `/ui/**` | MUC6 Vaadin admin UI (form login) |
| `/actuator/health`, `/actuator/caches`, `/actuator/metrics` | health public, rest ADMIN |
| *anything else* | proxied consumer traffic (Bearer JWT) |

Example policy:

```json
{"id": 2, "name": "regular-100-per-minute", "rate_limiting_strategy": "moving-window",
 "number_of_requests": 100, "time_period": "minute", "default_policy": false}
```

Errors (every 4xx/5xx of the proxy, including security errors):

```json
{"error": "You exceeded the request capacity for the current period", "resource": "orders/1", "https_status_code": 429}
```

| Status | When |
|---|---|
| 400 | validation errors, unknown enum codes |
| 401 | missing/invalid bearer token or admin credentials |
| 403 | valid token of an unregistered consumer |
| 404 | unknown entity / reserved path |
| 409 | uniqueness, overlap, last-consumer and in-use rules |
| 429 | rate limit exceeded (`Retry-After`, `X-RateLimit-Limit/Remaining/Policy/Strategy`) |
| 502 / 504 | target unreachable / timed out |
| 503 | Redis unavailable (fail closed) |

## Component responsibilities

| Component | Responsibility |
|---|---|
| `ProxyController` | catch-all endpoint, maps outcome to HTTP (200 relay or 429) |
| `ProxyRequestService` | evaluate, then forward or reject |
| `EvaluateRequestService` | resolve rule, build counter key, consume |
| `ApplicableRuleResolutionService` | DB-backed rule resolution (+ default fallback) |
| `CachingApplicableRuleResolver` | `@Cacheable` decorator (Caffeine) |
| `RedisBroadcastRuleCacheInvalidator` / `LocalRuleCacheEvictor` | `@CacheEvict` locally + pub/sub to other instances |
| `RuleAffectingTransaction` | commit configuration change, then invalidate |
| `*Service` (policies, consumers, ...) | CRUD use cases and business rules |
| `AssignmentConflictDetector`, `AssignmentSelector`, `ResourcePathMatcher` | pure domain rules |
| `RedisRateLimiterAdapter` | strategy dispatch, Redis failures -> 503 |
| `SlidingWindowLogAlgorithm` / `Bucket4jAlgorithm` | the three algorithms |
| `JdkHttpUpstreamForwarder`, `OpenApiCatalogAdapter` | target API access |
| `*PersistenceAdapter`, `TransactionTemplateUnitOfWork` | JPA persistence, transactions |
| `GlobalExceptionHandler`, `ExceptionLoggingFilter`, JSON security handlers | uniform JSON errors, logging with stack traces |

## Concurrency and multiple instances

* **Counters** live only in Redis. `moving-window` is one Lua script (trim, count, add, expire)
  that Redis executes atomically and that reads Redis' own clock (`TIME`), so instance clock skew
  does not matter. `fixed-window` and `token-bucket` use Bucket4j's Lettuce proxy manager, which
  updates bucket state with compare-and-swap and retries on contention. Two instances therefore
  never double-spend a token; this is proven by `DistributedRateLimitIT`, which drives two
  independently wired limiter instances concurrently for every strategy.
* **Configuration writes** serialise per consumer group with `SELECT ... FOR UPDATE` on the
  `consumer_groups` row, so concurrent assignment changes cannot both pass the overlap check and
  two deletions cannot both remove "not the last" consumer. Database triggers back this up.
* **Rule cache** is per instance (Caffeine). After the change commits, the instance evicts its
  own cache and publishes an invalidation on Redis; every other instance evicts on receipt.
  The TTL bounds staleness if a message is lost.
* **Request handling** runs on virtual threads (`spring.threads.virtual.enabled`), so blocking
  calls to Redis, PostgreSQL and the target are cheap; no reactive stack is needed.
