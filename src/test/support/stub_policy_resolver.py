from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


class StubPolicyResolver:
    def __init__(self, policy: ResolvedPolicy) -> None:
        self.policy = policy
        self.calls: list[tuple[ConsumerId, RequestTarget]] = []

    async def resolve(self, consumer_id: ConsumerId, target: RequestTarget) -> ResolvedPolicy:
        self.calls.append((consumer_id, target))
        return self.policy
