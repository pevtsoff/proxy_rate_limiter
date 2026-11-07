from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision


def rate_limit_headers(decision: RateLimitDecision) -> dict[str, str]:
    return {
        "X-RateLimit-Limit": str(decision.limit),
        "X-RateLimit-Remaining": str(max(decision.remaining, 0)),
        "X-RateLimit-Reset": str(int(decision.reset_at)),
    }
