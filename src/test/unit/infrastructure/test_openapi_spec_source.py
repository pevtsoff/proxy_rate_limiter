import httpx
import pytest

from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.errors.upstream_unavailable_error import UpstreamUnavailableError
from proxy.domain.value_objects.discovered_endpoint import DiscoveredEndpoint
from proxy.infrastructure.openapi_spec_source import OpenApiSpecSource
from target_api.app_factory import create_app as create_target_app


async def test_discovers_every_operation_of_the_target_api() -> None:
    transport = httpx.ASGITransport(app=create_target_app())
    async with httpx.AsyncClient(transport=transport, base_url="http://target") as client:
        endpoints = await OpenApiSpecSource(client, "/openapi.json").fetch_endpoints()
    assert len(endpoints) == 15
    assert DiscoveredEndpoint(HttpMethod.PATCH, "/carts/{item_id}") in endpoints
    assert DiscoveredEndpoint(HttpMethod.GET, "/orders") in endpoints


async def test_ignores_non_method_keys_of_path_items() -> None:
    document = {"paths": {"/x": {"parameters": [], "get": {}, "summary": "x"}}}
    transport = httpx.MockTransport(lambda request: httpx.Response(200, json=document))
    async with httpx.AsyncClient(transport=transport, base_url="http://target") as client:
        endpoints = await OpenApiSpecSource(client, "/openapi.json").fetch_endpoints()
    assert endpoints == [DiscoveredEndpoint(HttpMethod.GET, "/x")]


@pytest.mark.parametrize(
    "response", [httpx.Response(503), httpx.Response(200, content=b"not json")]
)
async def test_unusable_document_raises_502(response: httpx.Response) -> None:
    transport = httpx.MockTransport(lambda request: response)
    async with httpx.AsyncClient(transport=transport, base_url="http://target") as client:
        with pytest.raises(UpstreamUnavailableError):
            await OpenApiSpecSource(client, "/openapi.json").fetch_endpoints()
