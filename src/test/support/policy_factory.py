"""Builders for domain values used across tests."""

from proxy.domain.enums.rate_limiting_strategy import DEFAULT_STRATEGY, RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.domain.value_objects.rate_limit_rule import RateLimitRule
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


def resolved_policy(
    requests: int = 3,
    period: TimePeriod = TimePeriod.MINUTE,
    strategy: RateLimitingStrategy = DEFAULT_STRATEGY,
    scope_key: str = "assignment:1",
) -> ResolvedPolicy:
    return ResolvedPolicy("test-policy", RateLimitRule(requests, period, strategy), scope_key)
