from dataclasses import dataclass

from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision
from proxy.domain.value_objects.upstream_response import UpstreamResponse


@dataclass(frozen=True, slots=True)
class ProxyOutcome:
    """A forwarded response plus the rate limit state used for response headers."""

    response: UpstreamResponse
    decision: RateLimitDecision
