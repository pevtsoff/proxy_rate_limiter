from dataclasses import dataclass, replace

from proxy.domain.value_objects.consumer_id import ConsumerId

CONSUMER_HEADER = "x-consumer-id"


@dataclass(frozen=True, slots=True)
class ProxyRequest:
    """Framework-independent view of an incoming request to forward."""

    method: str
    path: str
    query: str
    headers: tuple[tuple[str, str], ...]
    body: bytes

    def header(self, name: str) -> str | None:
        wanted = name.lower()
        return next((value for key, value in self.headers if key.lower() == wanted), None)

    def with_consumer(self, consumer_id: ConsumerId) -> "ProxyRequest":
        kept = tuple((k, v) for k, v in self.headers if k.lower() != CONSUMER_HEADER)
        return replace(self, headers=(*kept, (CONSUMER_HEADER, consumer_id.value)))
