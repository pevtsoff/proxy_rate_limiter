from proxy.domain.value_objects.proxy_request import ProxyRequest
from proxy.domain.value_objects.upstream_response import UpstreamResponse


class RecordingForwarder:
    def __init__(self, response: UpstreamResponse) -> None:
        self.response = response
        self.forwarded: list[ProxyRequest] = []

    async def forward(self, request: ProxyRequest) -> UpstreamResponse:
        self.forwarded.append(request)
        return self.response
