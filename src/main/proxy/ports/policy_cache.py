from typing import Protocol

from proxy.domain.value_objects.resolved_policy import ResolvedPolicy
from proxy.ports.cache_invalidator import CacheInvalidator


class PolicyCache(CacheInvalidator, Protocol):
    async def get(self, key: str) -> ResolvedPolicy | None: ...

    async def put(self, key: str, policy: ResolvedPolicy) -> None: ...
