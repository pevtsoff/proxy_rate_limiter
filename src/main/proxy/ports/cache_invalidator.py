from typing import Protocol


class CacheInvalidator(Protocol):
    """Anything that must forget derived state when policy data changes."""

    async def invalidate(self) -> None: ...
