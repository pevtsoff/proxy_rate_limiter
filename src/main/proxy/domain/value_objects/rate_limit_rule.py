from dataclasses import dataclass

import limits
from limits import RateLimitItem

from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.domain.errors.domain_validation_error import DomainValidationError


@dataclass(frozen=True, slots=True)
class RateLimitRule:
    """`requests` per `period`, enforced with `strategy`."""

    requests: int
    period: TimePeriod
    strategy: RateLimitingStrategy

    def __post_init__(self) -> None:
        if self.requests < 1:
            raise DomainValidationError("A rate limit must allow at least one request")

    def to_expression(self) -> str:
        multiples, granularity = self.period.to_limits_window()
        return f"{self.requests} per {multiples} {granularity}"

    def to_limit_item(self) -> RateLimitItem:
        return limits.parse(self.to_expression())
