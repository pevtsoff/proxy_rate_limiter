"""Rate limiting strategies, discovered programmatically from the `limits` package.

`limits` is the engine slowapi is built on, so these are exactly slowapi's strategies.
"""

from enum import StrEnum

import limits.strategies

SUPPORTED_STRATEGIES: tuple[str, ...] = tuple(
    sorted(name.lower() for name in limits.strategies.STRATEGIES)
)


def _member_name(strategy: str) -> str:
    return strategy.upper().replace("-", "_")


RateLimitingStrategy = StrEnum(  # type: ignore[misc]
    "RateLimitingStrategy",
    {_member_name(strategy): strategy for strategy in SUPPORTED_STRATEGIES},
)

DEFAULT_STRATEGY = RateLimitingStrategy("moving-window")
