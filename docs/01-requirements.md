# 01 - Requirements and assumptions

## Understanding of the problem

The platform exposes REST APIs to mobile apps, web apps and internal services. We need a
service that lets API managers define rate-limiting policies, bind them to groups of consumers
for specific APIs, and enforce them on live traffic - consistently, even when the enforcing
service runs as several instances. Rejected callers must get a clear, machine-readable answer.

## 1. Assumptions

**Architectural assumption N1 - full reverse proxy, not middleware.** We do not know how the
target API is built (Python, Java, FastAPI, Spring, ...). The limiter is therefore a standalone
HTTP reverse proxy placed in front of the target, not a framework-specific filter. Consumers call
the proxy exactly as they would call the target.

**Architectural assumption N2 - the target publishes OpenAPI.** The proxy imports the list of
endpoints (method + path template) from the target's OpenAPI 3 document
(`TARGET_OPENAPI_PATH`, default `/v3/api-docs`).

Further assumptions:

| # | Assumption |
|---|---|
| A3 | Consumers authenticate with a bearer JWT whose `sub` claim is the `consumer_id`. A real identity provider is out of scope; the proxy can mint HS256 development tokens (`POST /admin/api/tokens`). |
| A4 | Limits are counted **per consumer**: "regular users may make 100 requests per minute" means each regular user, not the group as a whole. |
| A5 | A resource-group assignment shares **one budget across all resources of the group** for a consumer (that is the purpose of grouping). A resource assignment has its own budget. |
| A6 | The default policy applies per consumer **per resource**. Requests to paths that match no known resource share one per-consumer "unmatched" default budget, so varying ids or unknown paths cannot be used to bypass the limit. Unknown paths are still forwarded (the target answers 404 itself). |
| A7 | `group_name` is an enum (`premium`, `regular`, `free`, `internal`) and unique, i.e. one group per tier. |
| A8 | A consumer group is created together with its first consumer(s); deleting the last consumer of a group is refused (delete the group instead). Deleting a group deletes its consumers and assignments. |
| A9 | `consumer_id` is globally unique (one consumer belongs to exactly one group). |
| A10 | Resource sync is additive: new endpoints are inserted, existing resources are never overwritten (manual re-classification survives), endpoints that disappeared are reported as `stale` but kept because assignments may reference them. New resources go into a resource group named after the first path segment (`/orders/{id}` -> `orders`). |
| A11 | `io_type` defaults: GET/HEAD/OPTIONS/TRACE -> `read`; POST/PUT/PATCH/DELETE -> `write`. |
| A12 | When the counter store (Redis) is down the proxy **fails closed** (HTTP 503). An unmetered proxy would defeat its purpose; the trade-off is documented in 03. |
| A13 | Changing a policy starts fresh counters for it (its definition is part of the counter key). |
| A14 | The proxy terminates authentication: the consumer's `Authorization` header is not forwarded; the verified identity is passed as `X-Consumer-Id` (plus `X-Forwarded-*`). |
| A15 | Admins are a single configured account (role `ADMIN`) for both admin API and UI; user management is out of scope. |

## 2. Essential requirements

1. CRUD for policies, consumer groups, consumers, resource groups, resources and policy
   assignments (REST + admin UI with filtering and pagination).
2. Policy assignment per resource or per resource group, with the overlap rules enforced on
   every write (application check + database trigger).
3. A pre-created default policy covering every endpoint without an assignment.
4. Correct, atomic, distributed counting in Redis - exact under concurrency and across
   instances - for three strategies: `moving-window` (default), `fixed-window`, `token-bucket`.
5. HTTP 429 with the uniform JSON error (`error`, `resource`, `https_status_code`) and
   `Retry-After` / `X-RateLimit-*` headers.
6. Cached policy resolution (Caffeine LRU, size and TTL from `.env`) with eviction on every
   configuration change, on all instances.
7. One-command local start and test (`run.sh`, `test.sh`) with Docker only.

## 3. Ambiguities in the problem statement

* **"Consumer group should have at least one consumer" vs "consumer cannot exist without a
  group"** - a chicken-and-egg constraint. Resolved with a deferred (commit-time) constraint
  trigger and a create-group-with-consumers API.
* **`consumer_groups_policies.resource_id` points to resources *or* resource groups.** A
  polymorphic column cannot have a foreign key. The API keeps one `resource_id` field; the table
  stores it in `resource_id` or `resource_group_id` with real foreign keys and a CHECK that
  exactly one is set (see 03, D6).
* **Constraint 1 "either resource OR resource_group, not both"** - read as "per assignment row";
  a consumer group may mix resource-level and group-level assignments as long as they do not
  overlap (constraint 2 and MUC1 acceptance criterion 1 only make sense under this reading).
* **"LRU cache" titled "Redis caching" but acceptance criteria require Caffeine.** Implemented as
  a Caffeine cache per instance plus Redis pub/sub to invalidate all instances.
* **Cache key `(consumer_id, rest_path)`** - the HTTP method is added to the key because
  `GET /orders` and `POST /orders` are different resources with different policies.
* **"moving-window" default** - the term comes from the Python `limits` library. Bucket4j has no
  exact sliding window, so this strategy is implemented as a Redis Lua sliding log; Bucket4j
  backs `fixed-window` and `token-bucket` (see 03, D3).
* Whether a group budget is shared across the group's resources (A5) and whether limits are per
  consumer or per group (A4).

## 4. Questions for the product team

1. Per-consumer or per-group (pooled) quotas? Do we need both?
2. Should the default policy be one budget per consumer across all endpoints, or per endpoint?
3. Fail-open or fail-closed when Redis is unavailable? Per environment?
4. Should a policy change reset counters (current behaviour) or carry consumed quota over?
5. Burst allowances (e.g. 100/min but max 20/s) - multiple limits per policy?
6. Should limits also apply by IP for unauthenticated traffic?
7. Which identity provider issues consumer tokens in production (JWKS URL, claims)?
8. Is a quota-usage API or dashboard for consumers required?
9. Should stale resources be deleted automatically when they disappear from OpenAPI?
10. Audit log of configuration changes?

## 5. Out of scope

* Production identity provider integration, admin user management, RBAC beyond one ADMIN role.
* TLS termination, HTTP/2, WebSockets / streaming responses (bodies are buffered).
* Redis Cluster / Sentinel, PostgreSQL HA, Kubernetes, autoscaling.
* Quota analytics, billing, per-consumer overrides, IP-based limiting.
* Request/response transformation, caching of target responses, retries to the target.
