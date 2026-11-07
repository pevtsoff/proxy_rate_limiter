from typing import Protocol

from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


class PolicyResolver(Protocol):
    async def resolve(self, consumer_id: ConsumerId, target: RequestTarget) -> ResolvedPolicy: ...
