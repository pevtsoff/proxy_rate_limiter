"""Translation between Starlette requests/responses and the framework-free proxy model."""

from fastapi import Request
from fastapi.responses import Response

from proxy.api.rate_limit_headers import rate_limit_headers
from proxy.domain.value_objects.proxy_outcome import ProxyOutcome
from proxy.domain.value_objects.proxy_request import ProxyRequest


async def to_proxy_request(request: Request) -> ProxyRequest:
    return ProxyRequest(
        method=request.method,
        path=request.url.path,
        query=request.url.query,
        headers=tuple(request.headers.items()),
        body=await request.body(),
    )


def to_response(outcome: ProxyOutcome) -> Response:
    upstream = outcome.response
    response = Response(content=upstream.body, status_code=upstream.status_code)
    for name, value in upstream.headers:
        response.headers.append(name, value)
    for name, value in rate_limit_headers(outcome.decision).items():
        response.headers[name] = value
    return response
