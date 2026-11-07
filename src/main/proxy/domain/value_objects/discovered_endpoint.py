from dataclasses import dataclass

from proxy.domain.enums.http_method import HttpMethod


@dataclass(frozen=True, slots=True)
class DiscoveredEndpoint:
    """An operation found in the target API's OpenAPI document."""

    method: HttpMethod
    path: str
