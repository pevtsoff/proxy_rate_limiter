import limits.strategies

from proxy.domain.enums.rate_limiting_strategy import (
    DEFAULT_STRATEGY,
    SUPPORTED_STRATEGIES,
    RateLimitingStrategy,
)


def test_strategies_are_read_programmatically_from_limits() -> None:
    expected = sorted(name.lower() for name in limits.strategies.STRATEGIES)
    assert list(SUPPORTED_STRATEGIES) == expected
    assert sorted(s.value for s in RateLimitingStrategy) == expected


def test_default_strategy_is_moving_window() -> None:
    assert DEFAULT_STRATEGY.value == "moving-window"
