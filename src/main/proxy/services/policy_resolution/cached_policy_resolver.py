from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy
from proxy.ports.policy_cache import PolicyCache
from proxy.ports.policy_resolver import PolicyResolver


class CachedPolicyResolver:
    """LRU-cached policy lookup by (consumer_id, method, path) (MUC5 AC2).

    A decorator over any `PolicyResolver`: while policies do not change, requests are
    served from Redis without touching PostgreSQL.
    """

    def __init__(self, inner: PolicyResolver, cache: PolicyCache) -> None:
        self._inner = inner
        self._cache = cache

    async def resolve(self, consumer_id: ConsumerId, target: RequestTarget) -> ResolvedPolicy:
        key = target.cache_key(consumer_id)
        cached = await self._cache.get(key)
        if cached is not None:
            return cached
        resolved = await self._inner.resolve(consumer_id, target)
        await self._cache.put(key, resolved)
        return resolved
