from proxy.domain.errors.authentication_error import AuthenticationError
from proxy.domain.value_objects.consumer_id import ConsumerId


class StubTokenDecoder:
    """Accepts tokens of the form `valid:<consumer id>`."""

    def decode(self, token: str) -> ConsumerId:
        if not token.startswith("valid:"):
            raise AuthenticationError("Bearer token is invalid")
        return ConsumerId(token.removeprefix("valid:"))
