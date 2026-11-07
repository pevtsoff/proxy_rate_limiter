from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


class StubRateLimiter:
    def __init__(self, decision: RateLimitDecision) -> None:
        self.decision = decision
        self.hits: list[tuple[ResolvedPolicy, ConsumerId]] = []

    async def hit(self, policy: ResolvedPolicy, consumer_id: ConsumerId) -> RateLimitDecision:
        self.hits.append((policy, consumer_id))
        return self.decision
