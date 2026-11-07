from typing import Protocol

from proxy.domain.value_objects.proxy_request import ProxyRequest
from proxy.domain.value_objects.upstream_response import UpstreamResponse


class UpstreamForwarder(Protocol):
    async def forward(self, request: ProxyRequest) -> UpstreamResponse: ...
