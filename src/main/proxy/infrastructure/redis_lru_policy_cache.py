import time

from redis.asyncio import Redis

from proxy.domain.value_objects.lru_cache_config import LruCacheConfig
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy

ENTRY_PREFIX = "policy-cache:entry:"
RECENCY_KEY = "policy-cache:recency"


class RedisLruPolicyCache:
    """Shared LRU cache of resolved policies (MUC5).

    Entries are Redis strings with a TTL; a sorted set scored by last access time gives
    least-recently-used order, and entries beyond `capacity` are evicted on write.
    Living in Redis, the cache is shared by every proxy instance and invalidated once.
    """

    def __init__(self, redis: Redis, config: LruCacheConfig) -> None:
        self._redis = redis
        self._config = config

    async def get(self, key: str) -> ResolvedPolicy | None:
        raw = await self._redis.get(ENTRY_PREFIX + key)
        if raw is None:
            await self._redis.zrem(RECENCY_KEY, key)
            return None
        await self._redis.zadd(RECENCY_KEY, {key: time.time()}, xx=True)
        return ResolvedPolicy.from_json(raw)

    async def put(self, key: str, policy: ResolvedPolicy) -> None:
        async with self._redis.pipeline(transaction=True) as pipeline:
            pipeline.set(ENTRY_PREFIX + key, policy.to_json(), ex=self._config.ttl_seconds)
            pipeline.zadd(RECENCY_KEY, {key: time.time()})
            await pipeline.execute()
        await self._evict_overflow()

    async def invalidate(self) -> None:
        keys = [key async for key in self._redis.scan_iter(match=ENTRY_PREFIX + "*")]
        await self._redis.delete(RECENCY_KEY, *keys)

    async def size(self) -> int:
        return await self._redis.zcard(RECENCY_KEY)

    async def _evict_overflow(self) -> None:
        overflow = await self._redis.zcard(RECENCY_KEY) - self._config.capacity
        if overflow <= 0:
            return
        evicted = await self._redis.zpopmin(RECENCY_KEY, overflow)
        if evicted:
            await self._redis.delete(*(ENTRY_PREFIX + member for member, _ in evicted))
