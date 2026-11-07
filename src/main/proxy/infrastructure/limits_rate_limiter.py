from limits.aio.storage import Storage
from limits.aio.strategies import STRATEGIES
from limits.aio.strategies import RateLimiter as LimitsStrategy

from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy


class LimitsRateLimiter:
    """Counts requests with the `limits` engine (the library behind slowapi).

    Counters live in Redis, so every proxy instance enforces the same quota. Each policy
    may choose its own strategy, which a single slowapi `Limiter` cannot do.
    """

    def __init__(self, storage: Storage) -> None:
        self._strategies: dict[str, LimitsStrategy] = {
            name: strategy_class(storage) for name, strategy_class in STRATEGIES.items()
        }

    async def hit(self, policy: ResolvedPolicy, consumer_id: ConsumerId) -> RateLimitDecision:
        strategy = self._strategies[policy.rule.strategy.value]
        item = policy.rule.to_limit_item()
        identifiers = (consumer_id.value, policy.scope_key)
        allowed = await strategy.hit(item, *identifiers)
        stats = await strategy.get_window_stats(item, *identifiers)
        return RateLimitDecision(allowed, item.amount, stats.remaining, stats.reset_time)
