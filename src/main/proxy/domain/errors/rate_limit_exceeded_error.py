from proxy.domain.errors.proxy_error import ProxyError
from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision

RATE_LIMIT_EXCEEDED_MESSAGE = "You exceeded the request capacity for the current period"


class RateLimitExceededError(ProxyError):
    status_code = 429

    def __init__(self, resource: str, decision: RateLimitDecision) -> None:
        super().__init__(RATE_LIMIT_EXCEEDED_MESSAGE, resource)
        self.decision = decision
