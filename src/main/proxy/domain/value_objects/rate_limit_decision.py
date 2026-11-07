import math
import time
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RateLimitDecision:
    """Outcome of counting one request against its policy."""

    allowed: bool
    limit: int
    remaining: int
    reset_at: float

    def retry_after_seconds(self, now: float | None = None) -> int:
        current = time.time() if now is None else now
        return max(0, math.ceil(self.reset_at - current))
