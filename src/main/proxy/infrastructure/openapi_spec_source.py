from typing import Any

import httpx

from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.errors.upstream_unavailable_error import UpstreamUnavailableError
from proxy.domain.value_objects.discovered_endpoint import DiscoveredEndpoint


class OpenApiSpecSource:
    """Reads endpoints from the target API's swagger/OpenAPI JSON (assumption N2)."""

    def __init__(self, client: httpx.AsyncClient, openapi_path: str) -> None:
        self._client = client
        self._openapi_path = openapi_path

    async def fetch_endpoints(self) -> list[DiscoveredEndpoint]:
        document = await self._download()
        paths: dict[str, dict[str, Any]] = document.get("paths", {})
        return [
            DiscoveredEndpoint(HttpMethod(method.upper()), path)
            for path, operations in paths.items()
            for method in operations
            if HttpMethod.is_supported(method)
        ]

    async def _download(self) -> dict[str, Any]:
        try:
            response = await self._client.get(self._openapi_path)
            response.raise_for_status()
            return response.json()
        except (httpx.HTTPError, ValueError) as error:
            raise UpstreamUnavailableError(
                "Could not fetch the target API swagger document", self._openapi_path
            ) from error
