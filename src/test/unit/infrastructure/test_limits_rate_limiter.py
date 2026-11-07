import pytest

from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.infrastructure.limits_rate_limiter import LimitsRateLimiter
from proxy.infrastructure.rate_limit_storage_factory import create_rate_limit_storage
from support.policy_factory import resolved_policy

ALICE = ConsumerId("alice")


def _limiter() -> LimitsRateLimiter:
    return LimitsRateLimiter(create_rate_limit_storage("async+memory://"))


@pytest.mark.parametrize("strategy", list(RateLimitingStrategy))
async def test_each_strategy_allows_the_limit_then_rejects(strategy: RateLimitingStrategy) -> None:
    limiter = _limiter()
    policy = resolved_policy(requests=3, strategy=strategy)
    decisions = [await limiter.hit(policy, ALICE) for _ in range(4)]
    assert [d.allowed for d in decisions] == [True, True, True, False]
    assert decisions[0].limit == 3
    assert decisions[-1].remaining == 0


async def test_remaining_counts_down() -> None:
    limiter = _limiter()
    policy = resolved_policy(requests=3)
    remaining = [(await limiter.hit(policy, ALICE)).remaining for _ in range(3)]
    assert remaining == [2, 1, 0]


async def test_consumers_have_independent_quotas() -> None:
    limiter = _limiter()
    policy = resolved_policy(requests=1)
    assert (await limiter.hit(policy, ALICE)).allowed
    assert (await limiter.hit(policy, ConsumerId("bob"))).allowed
    assert not (await limiter.hit(policy, ALICE)).allowed


async def test_scopes_have_independent_quotas() -> None:
    limiter = _limiter()
    assert (await limiter.hit(resolved_policy(requests=1, scope_key="a"), ALICE)).allowed
    assert (await limiter.hit(resolved_policy(requests=1, scope_key="b"), ALICE)).allowed


def test_sync_storage_url_is_rejected() -> None:
    with pytest.raises(ValueError, match="async"):
        create_rate_limit_storage("memory://")
