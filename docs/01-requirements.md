# 01 — Requirements and assumptions

## Understanding of the problem

The platform exposes REST APIs to mobile apps, web apps and internal services. We need a
service that lets API managers define request policies, attach them to consumers and
APIs, evaluate every incoming request against the applicable policy and tell the caller
clearly when a request is rejected — consistently, even with several instances running.

## 1. Assumptions

**Architectural assumption N1 — a proxy, not a middleware.** We do not know how the
target API is built (Python, Java, FastAPI, Spring…), so the limiter is a standalone
reverse proxy in front of it, not a library embedded in it. Any HTTP API can be protected
without code changes; the cost is one extra network hop.

**Architectural assumption N2 — the target API publishes OpenAPI (swagger) docs.** The
proxy imports the target's endpoints and methods from its OpenAPI JSON into the
`resources` table (`POST /admin/resources/sync`).

Further assumptions:

* Consumers authenticate with a bearer token; the consumer id is the JWT `sub` claim.
  Tokens are HS256 JWTs signed with a shared secret. A real deployment would validate
  tokens from the platform's identity provider instead; `POST /admin/tokens` exists only
  to mint test tokens.
* Policies are only assigned to **consumer groups**, never to individual consumers.
* A consumer belongs to exactly one group; a group always has at least one consumer.
* Consumer group names are user-defined unique strings; duplicate names are rejected.
* Quota is counted **per consumer**: two consumers of the same group never share a bucket.
* A resource-group assignment gives a consumer **one shared quota for all resources in the
  group**; an all-resources assignment gives one shared quota for everything.
* Requests whose path is not in `resources` are still forwarded (the target decides what
  to answer) and are limited by the default policy with one shared bucket, so varying the
  path cannot dodge the limit.
* Precedence when several assignments could apply: resource › resource group ›
  all resources › default policy.
* A policy's window is one of second, minute, hour, day, week (a week is counted as 7 days).
* The management API is internal and not authenticated (see out of scope); it is only
  protected by a per-IP slowapi limit.

## 2. Essential requirements

* CRUD for consumer groups, consumers, policies, resource groups and policy assignments,
  through the REST API **and** an Admin UI with the same rules (MUC1, MUC2, MUC4, MUC6).
* Import of target endpoints from swagger (MUC1.4).
* Conflict rules for assignments: for one consumer group, a resource is covered either by
  its own assignment or by its group's assignment, never both (MUC1 AC1).
* A pre-created default policy that covers every endpoint when nothing else applies (MUC3).
* Correct enforcement across several proxy instances: counters live in Redis (task §1).
* LRU cache of policy resolution with configurable capacity and TTL, invalidated on any
  change (MUC5).
* A meaningful JSON 429 with the resource path; the same JSON shape for every error.
* All exceptions logged with module, function, line and traceback.
* One-command start (`./run.sh`) and tests (`./test.sh`) on a laptop.

## 3. Ambiguities in the problem statement

| Ambiguity | Decision |
|---|---|
| "Extract the target endpoint path and method from the bearer token" (CUC1.2) | The token identifies the consumer; method and path come from the request line. |
| `consumer_groups.consumer_id` non-null **and** "consumer cannot exist without a group" is circular | No `consumer_id` column on groups. A group is created together with its first consumers in one transaction; the services refuse to remove a group's last consumer. |
| `consumer_groups_policies.resource_id` points to resources *or* resource groups | Two typed nullable FKs (`resource_id`, `resource_group_id`) plus a CHECK, so PostgreSQL keeps referential integrity. |
| "Either resource OR resource_group, NOT BOTH" — per group, or per resource? | Per resource: a group may mix resource and resource-group assignments as long as they never overlap (consistent with AC1 "vice versa"). |
| How to express "one policy for ALL resources" | An extra `all_resources` relationship type with no target. |
| Is the cache key `(consumer_id, rest_path)` method-aware? | Yes: `(consumer_id, method, path)`, because resources are method-specific. |
| What happens to consumers when their group is deleted? | They are deleted too (they cannot exist without a group); assignments of the group are deleted as well. |
| slowapi is decorator-based, the proxy's routes are dynamic | Per-policy enforcement uses `limits` (the engine of slowapi) directly; slowapi guards the management API. See 03-design-decisions. |
| "time_period" has no month | Only second, minute, hour, day, week are offered, as specified. |

## 4. Questions for the product team

* Which identity provider issues consumer tokens, and which claim holds the consumer id?
* Should limits apply per consumer, per group (shared tier budget) or both at once?
* Should unknown paths be blocked instead of forwarded?
* Do we need several simultaneous limits per resource (e.g. 10/second **and** 1000/day)?
* Should rejected requests count against the quota? (Today they do not: a hit on a full
  bucket is rejected without consuming.)
* What is the expected behaviour when Redis is unavailable — fail open or fail closed?
* Is a short window after a policy change, in which an old policy may still be applied,
  acceptable? (See cache trade-offs.)
* Do API managers need audit history of policy changes?
* Should internal consumers bypass limits entirely?

## 5. Out of scope

* Authentication/authorisation of the management API and Admin UI.
* A real identity provider; key rotation; token revocation.
* TLS termination, HTTP/2, WebSockets and streaming bodies (bodies are buffered).
* Redis high availability (Sentinel/Cluster) and the fail-open/closed policy.
* Request quotas by payload size, cost-based limits, concurrency limits.
* Multi-target routing (one proxy fronts exactly one target API).
* Production deployment (Kubernetes, cloud), observability stack beyond logs.
