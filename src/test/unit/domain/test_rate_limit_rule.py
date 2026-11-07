import pytest

from proxy.domain.enums.rate_limiting_strategy import DEFAULT_STRATEGY
from proxy.domain.enums.time_period import TimePeriod
from proxy.domain.errors.domain_validation_error import DomainValidationError
from proxy.domain.value_objects.rate_limit_rule import RateLimitRule


def test_10_per_minute_becomes_a_limits_item_of_10_per_60_seconds() -> None:
    item = RateLimitRule(10, TimePeriod.MINUTE, DEFAULT_STRATEGY).to_limit_item()
    assert item.amount == 10
    assert item.get_expiry() == 60


def test_weekly_rule_expires_after_seven_days() -> None:
    item = RateLimitRule(5, TimePeriod.WEEK, DEFAULT_STRATEGY).to_limit_item()
    assert item.get_expiry() == 7 * 86_400


@pytest.mark.parametrize("requests", [0, -1])
def test_rule_must_allow_at_least_one_request(requests: int) -> None:
    with pytest.raises(DomainValidationError):
        RateLimitRule(requests, TimePeriod.MINUTE, DEFAULT_STRATEGY)
