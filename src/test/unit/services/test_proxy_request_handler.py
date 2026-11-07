import pytest

from proxy.domain.errors.authentication_error import AuthenticationError
from proxy.domain.errors.rate_limit_exceeded_error import RateLimitExceededError
from proxy.domain.value_objects.proxy_request import ProxyRequest
from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision
from proxy.domain.value_objects.upstream_response import UpstreamResponse
from proxy.services.proxying.bearer_authenticator import BearerAuthenticator
from proxy.services.proxying.proxy_request_handler import ProxyRequestHandler
from support.policy_factory import resolved_policy
from support.recording_forwarder import RecordingForwarder
from support.stub_policy_resolver import StubPolicyResolver
from support.stub_rate_limiter import StubRateLimiter
from support.stub_token_decoder import StubTokenDecoder

ALLOWED = RateLimitDecision(True, 3, 2, 0.0)
DENIED = RateLimitDecision(False, 3, 0, 0.0)
OK = UpstreamResponse(200, (("content-type", "application/json"),), b"{}")


def _request(authorization: str | None = "Bearer valid:alice") -> ProxyRequest:
    headers = (("authorization", authorization),) if authorization else ()
    return ProxyRequest("GET", "/orders/1", "", headers, b"")


def _handler(decision: RateLimitDecision) -> tuple[ProxyRequestHandler, RecordingForwarder]:
    forwarder = RecordingForwarder(OK)
    handler = ProxyRequestHandler(
        BearerAuthenticator(StubTokenDecoder()),
        StubPolicyResolver(resolved_policy()),
        StubRateLimiter(decision),
        forwarder,
    )
    return handler, forwarder


async def test_allowed_request_is_forwarded_with_consumer_header() -> None:
    handler, forwarder = _handler(ALLOWED)
    outcome = await handler.handle(_request())
    assert outcome.response is OK
    assert outcome.decision is ALLOWED
    assert forwarder.forwarded[0].header("x-consumer-id") == "alice"


async def test_exhausted_quota_raises_429_and_does_not_forward() -> None:
    handler, forwarder = _handler(DENIED)
    with pytest.raises(RateLimitExceededError) as error:
        await handler.handle(_request())
    assert error.value.resource == "orders/1"
    assert forwarder.forwarded == []


async def test_missing_token_is_rejected_before_counting() -> None:
    handler, forwarder = _handler(ALLOWED)
    with pytest.raises(AuthenticationError):
        await handler.handle(_request(authorization=None))
    assert forwarder.forwarded == []
