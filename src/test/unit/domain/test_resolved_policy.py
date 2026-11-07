from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy
from support.policy_factory import resolved_policy


def test_json_round_trip_preserves_every_field() -> None:
    policy = resolved_policy(7, TimePeriod.HOUR, RateLimitingStrategy("fixed-window"), "scope")
    assert ResolvedPolicy.from_json(policy.to_json()) == policy
