from dataclasses import dataclass

from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.path_template import normalize_path


@dataclass(frozen=True, slots=True)
class RequestTarget:
    """The method and concrete path a consumer tries to reach, e.g. `GET /orders/1`."""

    method: HttpMethod
    path: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "path", normalize_path(self.path))

    @property
    def resource_label(self) -> str:
        """Path as shown to callers in error bodies, e.g. `orders/1`."""
        return self.path.lstrip("/")

    def cache_key(self, consumer_id: ConsumerId) -> str:
        return f"{consumer_id.value}|{self.method.value}|{self.path}"
