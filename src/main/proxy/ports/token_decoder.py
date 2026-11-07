from typing import Protocol

from proxy.domain.value_objects.consumer_id import ConsumerId


class TokenDecoder(Protocol):
    def decode(self, token: str) -> ConsumerId: ...
