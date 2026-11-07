from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision


def test_retry_after_rounds_up_to_whole_seconds() -> None:
    decision = RateLimitDecision(False, 10, 0, reset_at=100.2)
    assert decision.retry_after_seconds(now=95.0) == 6


def test_retry_after_is_never_negative() -> None:
    assert RateLimitDecision(False, 10, 0, reset_at=1.0).retry_after_seconds(now=5.0) == 0
