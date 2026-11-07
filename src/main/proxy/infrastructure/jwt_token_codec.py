from datetime import UTC, datetime, timedelta
from typing import Any

import jwt

from proxy.domain.errors.authentication_error import AuthenticationError
from proxy.domain.errors.domain_validation_error import DomainValidationError
from proxy.domain.value_objects.consumer_id import ConsumerId


class JwtTokenCodec:
    """HS256 JWTs whose `sub` claim is the consumer id."""

    def __init__(self, secret: str, algorithm: str) -> None:
        self._secret = secret
        self._algorithm = algorithm

    def encode(self, consumer_id: str, ttl_seconds: int) -> str:
        issued_at = datetime.now(UTC)
        claims = {
            "sub": consumer_id,
            "iat": issued_at,
            "exp": issued_at + timedelta(seconds=ttl_seconds),
        }
        return jwt.encode(claims, self._secret, algorithm=self._algorithm)

    def decode(self, token: str) -> ConsumerId:
        claims = self._verified_claims(token)
        try:
            return ConsumerId(str(claims["sub"]))
        except DomainValidationError as error:
            raise AuthenticationError("Bearer token has an invalid subject") from error

    def _verified_claims(self, token: str) -> dict[str, Any]:
        try:
            return jwt.decode(
                token,
                self._secret,
                algorithms=[self._algorithm],
                options={"require": ["sub", "exp"]},
            )
        except jwt.ExpiredSignatureError as error:
            raise AuthenticationError("Bearer token has expired") from error
        except jwt.InvalidTokenError as error:
            raise AuthenticationError("Bearer token is invalid") from error
