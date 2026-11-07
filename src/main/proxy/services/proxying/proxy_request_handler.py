from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.errors.rate_limit_exceeded_error import RateLimitExceededError
from proxy.domain.value_objects.proxy_outcome import ProxyOutcome
from proxy.domain.value_objects.proxy_request import ProxyRequest
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.ports.policy_resolver import PolicyResolver
from proxy.ports.rate_limiter import RateLimiter
from proxy.ports.upstream_forwarder import UpstreamForwarder
from proxy.services.proxying.bearer_authenticator import BearerAuthenticator


class ProxyRequestHandler:
    """Use case CUC1: authenticate, resolve the policy, count the hit, then forward or reject."""

    def __init__(
        self,
        authenticator: BearerAuthenticator,
        resolver: PolicyResolver,
        rate_limiter: RateLimiter,
        forwarder: UpstreamForwarder,
    ) -> None:
        self._authenticator = authenticator
        self._resolver = resolver
        self._rate_limiter = rate_limiter
        self._forwarder = forwarder

    async def handle(self, request: ProxyRequest) -> ProxyOutcome:
        target = RequestTarget(HttpMethod(request.method.upper()), request.path)
        authorization = request.header("authorization")
        consumer_id = self._authenticator.authenticate(authorization, target.resource_label)
        policy = await self._resolver.resolve(consumer_id, target)
        decision = await self._rate_limiter.hit(policy, consumer_id)
        if not decision.allowed:
            raise RateLimitExceededError(target.resource_label, decision)
        response = await self._forwarder.forward(request.with_consumer(consumer_id))
        return ProxyOutcome(response, decision)
