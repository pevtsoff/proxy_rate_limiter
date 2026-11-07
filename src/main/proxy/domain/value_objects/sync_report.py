from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class SyncReport:
    """Result of synchronising resources from the target API's swagger document."""

    discovered: int
    created: int
