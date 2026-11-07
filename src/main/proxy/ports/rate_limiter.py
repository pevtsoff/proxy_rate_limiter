from typing import Protocol

from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


class RateLimiter(Protocol):
    async def hit(self, policy: ResolvedPolicy, consumer_id: ConsumerId) -> RateLimitDecision: ...
