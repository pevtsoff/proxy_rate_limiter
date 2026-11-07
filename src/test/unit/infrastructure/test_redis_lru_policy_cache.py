import pytest
from fakeredis import FakeAsyncRedis

from proxy.domain.value_objects.lru_cache_config import LruCacheConfig
from proxy.infrastructure.redis_lru_policy_cache import ENTRY_PREFIX, RedisLruPolicyCache
from support.policy_factory import resolved_policy


@pytest.fixture
async def redis() -> FakeAsyncRedis:
    return FakeAsyncRedis(decode_responses=True)


async def test_put_then_get_returns_the_policy(redis: FakeAsyncRedis) -> None:
    cache = RedisLruPolicyCache(redis, LruCacheConfig(10, 60))
    await cache.put("k", resolved_policy())
    assert await cache.get("k") == resolved_policy()


async def test_miss_returns_none(redis: FakeAsyncRedis) -> None:
    assert await RedisLruPolicyCache(redis, LruCacheConfig(10, 60)).get("missing") is None


async def test_entries_expire_after_ttl(redis: FakeAsyncRedis) -> None:
    cache = RedisLruPolicyCache(redis, LruCacheConfig(10, 60))
    await cache.put("k", resolved_policy())
    assert 0 < await redis.ttl(ENTRY_PREFIX + "k") <= 60


async def test_least_recently_used_entry_is_evicted_beyond_capacity(
    redis: FakeAsyncRedis,
) -> None:
    cache = RedisLruPolicyCache(redis, LruCacheConfig(2, 60))
    await cache.put("a", resolved_policy())
    await cache.put("b", resolved_policy())
    await cache.get("a")
    await cache.put("c", resolved_policy())
    assert await cache.get("b") is None
    assert await cache.get("a") is not None
    assert await cache.get("c") is not None
    assert await cache.size() == 2


async def test_invalidate_drops_everything(redis: FakeAsyncRedis) -> None:
    cache = RedisLruPolicyCache(redis, LruCacheConfig(10, 60))
    await cache.put("a", resolved_policy())
    await cache.put("b", resolved_policy())
    await cache.invalidate()
    assert await cache.size() == 0
    assert await cache.get("a") is None
