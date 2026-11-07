import json
from dataclasses import dataclass

from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.domain.value_objects.rate_limit_rule import RateLimitRule


@dataclass(frozen=True, slots=True)
class ResolvedPolicy:
    """The policy that applies to one (consumer, request target) pair.

    `scope_key` names the counter bucket: requests sharing a scope key share quota.
    """

    policy_name: str
    rule: RateLimitRule
    scope_key: str

    def to_json(self) -> str:
        return json.dumps(
            {
                "policy_name": self.policy_name,
                "requests": self.rule.requests,
                "period": self.rule.period.value,
                "strategy": self.rule.strategy.value,
                "scope_key": self.scope_key,
            }
        )

    @classmethod
    def from_json(cls, raw: str) -> "ResolvedPolicy":
        payload = json.loads(raw)
        rule = RateLimitRule(
            requests=payload["requests"],
            period=TimePeriod(payload["period"]),
            strategy=RateLimitingStrategy(payload["strategy"]),
        )
        return cls(payload["policy_name"], rule, payload["scope_key"])
