from typing import Protocol

from proxy.domain.value_objects.discovered_endpoint import DiscoveredEndpoint


class ApiSpecSource(Protocol):
    """Where the target API's endpoints are discovered from (its swagger document)."""

    async def fetch_endpoints(self) -> list[DiscoveredEndpoint]: ...
