# Design decisions and trade-offs

- **Standalone proxy:** chosen because the target can be written in any language. Middleware inside each target would couple policy enforcement to the service stack.
- **FastAPI + async SQLAlchemy/HTTPX/Redis:** supports concurrent I/O while keeping the example small and locally runnable.
- **Redis counters:** Redis `INCR` and expiration provide a simple atomic shared counter. Fixed windows can allow a burst across a boundary; a true moving-window algorithm should use Redis sorted sets/Lua.
- **Local bounded LRU plus Redis cache keys:** local LRU accelerates policy lookups; Redis keys provide inspectable shared cache entries. The source-of-truth lookup currently queries PostgreSQL on local cache miss.
- **Polymorphic assignment:** a single relationship table serves resource and resource-group rules. This is compact but loses a direct FK for `resource_id`; API checks validate it.
- **SQLAdmin:** gives maintainers a usable CRUD interface quickly. The local sample has no authentication integration; deployments must protect it.
- **SlowAPI:** included as an explicit dependency per requested stack. Dynamic per-consumer policies are enforced by the proxy's Redis counter because SlowAPI decorators are static per route; the configured strategy field is not yet algorithmically honored.
- **Schema creation:** SQLAlchemy creates tables on startup for minimum local setup. Use versioned migrations before production.
