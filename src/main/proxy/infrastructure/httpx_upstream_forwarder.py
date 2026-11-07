from collections.abc import Iterable

import httpx

from proxy.domain.errors.upstream_unavailable_error import UpstreamUnavailableError
from proxy.domain.value_objects.proxy_request import ProxyRequest
from proxy.domain.value_objects.upstream_response import UpstreamResponse

HOP_BY_HOP_HEADERS = frozenset(
    {
        "connection",
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailers",
        "transfer-encoding",
        "upgrade",
        "host",
        "content-length",
    }
)
# httpx already decoded the body, so the original encoding header would be wrong.
EXCLUDED_RESPONSE_HEADERS = HOP_BY_HOP_HEADERS | {"content-encoding"}


def _filter_headers(
    headers: Iterable[tuple[str, str]], excluded: frozenset[str]
) -> tuple[tuple[str, str], ...]:
    return tuple((name, value) for name, value in headers if name.lower() not in excluded)


class HttpxUpstreamForwarder:
    """Forwards a request unchanged (minus hop-by-hop headers) to the target API."""

    def __init__(self, client: httpx.AsyncClient) -> None:
        self._client = client

    async def forward(self, request: ProxyRequest) -> UpstreamResponse:
        try:
            upstream = await self._client.request(
                request.method,
                self._relative_url(request),
                headers=list(_filter_headers(request.headers, HOP_BY_HOP_HEADERS)),
                content=request.body,
            )
        except httpx.HTTPError as error:
            raise UpstreamUnavailableError(
                "The target API is unavailable", request.path.lstrip("/")
            ) from error
        headers = _filter_headers(upstream.headers.multi_items(), EXCLUDED_RESPONSE_HEADERS)
        return UpstreamResponse(upstream.status_code, headers, upstream.content)

    @staticmethod
    def _relative_url(request: ProxyRequest) -> str:
        return f"{request.path}?{request.query}" if request.query else request.path
