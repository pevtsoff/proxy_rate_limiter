# Rate limiter proxy sequence diagram

The proxy authenticates the bearer token, resolves the consumer's policy, and checks the shared rate-limit counter. If the request is allowed, it forwards it to the target API; otherwise, it returns a `429` response without forwarding.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant R as FastAPI proxy route
    participant H as ProxyRequestHandler
    participant A as BearerAuthenticator
    participant T as TokenDecoder
    participant PR as CachedPolicyResolver
    participant Cache as Redis policy cache
    participant DR as DatabasePolicyResolver
    participant DB as PostgreSQL
    participant L as LimitsRateLimiter
    participant RL as Redis rate-limit counters
    participant F as HttpxUpstreamForwarder
    participant API as Target API

    C->>R: HTTP request + Bearer token
    R->>R: Convert request to ProxyRequest
    R->>H: handle(proxy_request)
    H->>A: authenticate(Authorization, resource)
    A->>T: decode(token)
    T-->>A: consumer_id
    A-->>H: consumer_id

    H->>PR: resolve(consumer_id, method, path)
    PR->>Cache: Look up policy cache key
    alt Cache hit
        Cache-->>PR: Resolved policy
    else Cache miss
        PR->>DR: Resolve policy
        DR->>DB: Find consumer and group
        DR->>DB: Locate matching resource and assignments
        DR->>DB: Select applicable policy
        Note over DR,DB: If no assignment matches, use the default policy
        DB-->>DR: Consumer, resource, assignment and policy data
        DR-->>PR: Resolved policy and quota scope
        PR->>Cache: Store resolved policy
    end
    PR-->>H: Resolved policy

    H->>L: hit(policy, consumer_id)
    L->>RL: Increment/check shared quota counter
    RL-->>L: Allowed? Remaining quota and reset time
    L-->>H: RateLimitDecision

    alt Quota exceeded
        H-->>R: Raise rate-limit error
        R-->>C: 429 + rate-limit headers + Retry-After
    else Quota allowed
        H->>F: Forward request with consumer ID
        F->>API: HTTP request
        API-->>F: Upstream response
        F-->>H: Response body, status and headers
        H-->>R: ProxyOutcome
        R->>R: Convert to HTTP response and add rate-limit headers
        R-->>C: Upstream response + rate-limit headers
    end
```
