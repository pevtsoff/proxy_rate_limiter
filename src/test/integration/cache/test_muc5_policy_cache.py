import dataclasses

import httpx
import pytest

from proxy.api.app_container import AppContainer
from proxy.app_factory import create_app
from proxy.domain.value_objects.lru_cache_config import LruCacheConfig
from proxy.infrastructure.redis_lru_policy_cache import ENTRY_PREFIX, RedisLruPolicyCache
from proxy.seeding import seed_plan
from proxy.services.policy_resolution.database_policy_resolver import DatabasePolicyResolver
from support.api_helpers import find_by_name

pytestmark = pytest.mark.usefixtures("seeded")


async def _warm(client: httpx.AsyncClient, auth_headers, consumer: str = "free-user-1") -> None:
    assert (await client.get("/orders/1", headers=auth_headers(consumer))).status_code == 200


async def test_muc5_resolved_policy_is_cached_with_configured_ttl(
    client: httpx.AsyncClient, container: AppContainer, auth_headers
) -> None:
    await _warm(client, auth_headers)
    assert await container.policy_cache.size() == 1
    ttl = await container.redis.ttl(f"{ENTRY_PREFIX}free-user-1|GET|/orders/1")
    assert 0 < ttl <= container.settings.lru_cache_ttl


async def test_muc5_cached_requests_do_not_read_the_database(
    client: httpx.AsyncClient, auth_headers, monkeypatch: pytest.MonkeyPatch
) -> None:
    await _warm(client, auth_headers)

    async def database_must_not_be_used(*args: object) -> None:
        raise AssertionError("policy should come from the cache")

    monkeypatch.setattr(DatabasePolicyResolver, "resolve", database_must_not_be_used)
    await _warm(client, auth_headers)


async def test_muc5_policy_change_invalidates_the_cache(
    client: httpx.AsyncClient, container: AppContainer, auth_headers
) -> None:
    await _warm(client, auth_headers)
    policy = await find_by_name(client, "/admin/policies", "name", seed_plan.FREE_POLICY)
    await client.patch(f"/admin/policies/{policy['id']}", json={"number_of_requests": 1})
    assert await container.policy_cache.size() == 0
    response = await client.get("/orders/1", headers=auth_headers("free-user-1"))
    assert response.headers["X-RateLimit-Limit"] == "1"


async def test_muc5_assignment_change_invalidates_the_cache(
    client: httpx.AsyncClient, container: AppContainer, auth_headers
) -> None:
    await _warm(client, auth_headers)
    assignments = (await client.get("/admin/consumer-groups-policies")).json()
    await client.delete(f"/admin/consumer-groups-policies/{assignments[0]['id']}")
    assert await container.policy_cache.size() == 0


async def test_muc5_cache_evicts_least_recently_used_beyond_capacity(
    container: AppContainer, auth_headers
) -> None:
    tiny_cache = RedisLruPolicyCache(container.redis, LruCacheConfig(capacity=2, ttl_seconds=60))
    app = create_app(dataclasses.replace(container, policy_cache=tiny_cache))
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://proxy"
    ) as client:
        for path in ("/orders/1", "/orders/2", "/orders/3"):
            await client.get(path, headers=auth_headers("internal-service-1"))
    assert await tiny_cache.size() == 2
    assert await tiny_cache.get("internal-service-1|GET|/orders/1") is None
