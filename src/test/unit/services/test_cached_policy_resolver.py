from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.services.policy_resolution.cached_policy_resolver import CachedPolicyResolver
from support.fake_policy_cache import FakePolicyCache
from support.policy_factory import resolved_policy
from support.stub_policy_resolver import StubPolicyResolver

TARGET = RequestTarget(HttpMethod.GET, "/orders/1")
ALICE = ConsumerId("alice")


async def test_second_lookup_is_served_from_cache() -> None:
    inner = StubPolicyResolver(resolved_policy())
    resolver = CachedPolicyResolver(inner, FakePolicyCache())
    first = await resolver.resolve(ALICE, TARGET)
    second = await resolver.resolve(ALICE, TARGET)
    assert first == second
    assert len(inner.calls) == 1


async def test_cache_is_keyed_by_consumer_and_path() -> None:
    inner = StubPolicyResolver(resolved_policy())
    resolver = CachedPolicyResolver(inner, FakePolicyCache())
    await resolver.resolve(ALICE, TARGET)
    await resolver.resolve(ConsumerId("bob"), TARGET)
    await resolver.resolve(ALICE, RequestTarget(HttpMethod.GET, "/orders/2"))
    assert len(inner.calls) == 3


async def test_invalidation_forces_a_fresh_lookup() -> None:
    inner = StubPolicyResolver(resolved_policy())
    cache = FakePolicyCache()
    resolver = CachedPolicyResolver(inner, cache)
    await resolver.resolve(ALICE, TARGET)
    await cache.invalidate()
    await resolver.resolve(ALICE, TARGET)
    assert len(inner.calls) == 2
