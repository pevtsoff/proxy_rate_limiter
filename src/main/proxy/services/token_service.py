from proxy.infrastructure.jwt_token_codec import JwtTokenCodec
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.token_create import TokenCreate
from proxy.schemas.token_read import TokenRead


class TokenService:
    """Issues bearer tokens for registered consumers (a stand-in for a real identity provider)."""

    def __init__(self, uow: SqlAlchemyUnitOfWork, codec: JwtTokenCodec, default_ttl: int) -> None:
        self._uow = uow
        self._codec = codec
        self._default_ttl = default_ttl

    async def issue(self, command: TokenCreate) -> TokenRead:
        consumer = await self._uow.consumers.get_or_raise(command.consumer_id)
        ttl = command.ttl_seconds or self._default_ttl
        token = self._codec.encode(consumer.id, ttl)
        return TokenRead(access_token=token, expires_in=ttl)
