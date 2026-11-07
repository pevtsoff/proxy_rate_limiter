from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


class FakePolicyCache:
    def __init__(self) -> None:
        self.entries: dict[str, ResolvedPolicy] = {}
        self.invalidations = 0

    async def get(self, key: str) -> ResolvedPolicy | None:
        return self.entries.get(key)

    async def put(self, key: str, policy: ResolvedPolicy) -> None:
        self.entries[key] = policy

    async def invalidate(self) -> None:
        self.invalidations += 1
        self.entries.clear()
