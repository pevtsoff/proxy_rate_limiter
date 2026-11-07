from proxy.domain.errors.authentication_error import AuthenticationError
from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.ports.token_decoder import TokenDecoder

BEARER_PREFIX = "bearer "


class BearerAuthenticator:
    """Extracts the consumer id from an `Authorization: Bearer <token>` header (CUC1.1)."""

    def __init__(self, decoder: TokenDecoder) -> None:
        self._decoder = decoder

    def authenticate(self, authorization: str | None, resource: str) -> ConsumerId:
        token = self._extract_token(authorization, resource)
        try:
            return self._decoder.decode(token)
        except AuthenticationError as error:
            error.resource = resource
            raise

    @staticmethod
    def _extract_token(authorization: str | None, resource: str) -> str:
        if not authorization or not authorization.lower().startswith(BEARER_PREFIX):
            raise AuthenticationError("Missing bearer token", resource)
        token = authorization[len(BEARER_PREFIX) :].strip()
        if not token:
            raise AuthenticationError("Missing bearer token", resource)
        return token
