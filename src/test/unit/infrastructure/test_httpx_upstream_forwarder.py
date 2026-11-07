import httpx
import pytest

from proxy.domain.errors.upstream_unavailable_error import UpstreamUnavailableError
from proxy.domain.value_objects.proxy_request import ProxyRequest
from proxy.infrastructure.httpx_upstream_forwarder import HttpxUpstreamForwarder

REQUEST = ProxyRequest(
    method="POST",
    path="/orders",
    query="debug=1",
    headers=(("host", "proxy"), ("connection", "keep-alive"), ("x-trace", "abc")),
    body=b'{"name": "pen"}',
)


async def test_forwards_method_path_query_body_and_end_to_end_headers() -> None:
    seen: list[httpx.Request] = []

    def respond(request: httpx.Request) -> httpx.Response:
        seen.append(request)
        return httpx.Response(201, json={"ok": True}, headers={"x-upstream": "1"})

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(respond), base_url="http://target"
    ) as client:
        response = await HttpxUpstreamForwarder(client).forward(REQUEST)
    forwarded = seen[0]
    assert (forwarded.method, forwarded.url.path) == ("POST", "/orders")
    assert forwarded.url.query == b"debug=1"
    assert forwarded.content == REQUEST.body
    assert forwarded.headers["x-trace"] == "abc"
    assert forwarded.headers["host"] == "target"
    assert response.status_code == 201
    assert ("x-upstream", "1") in response.headers


async def test_connection_failure_becomes_502() -> None:
    def fail(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused", request=request)

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(fail), base_url="http://target"
    ) as client:
        with pytest.raises(UpstreamUnavailableError):
            await HttpxUpstreamForwarder(client).forward(REQUEST)
