from collections.abc import Sequence
from typing import Any

from sqlalchemy import Select, select
from sqlalchemy.ext.asyncio import AsyncSession

from proxy.db.base import Base
from proxy.domain.errors.not_found_error import NotFoundError


class SqlAlchemyRepository[ModelT: Base]:
    """Generic persistence operations shared by every aggregate repository."""

    model: type[ModelT]

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def get(self, entity_id: Any) -> ModelT | None:
        return await self._session.get(self.model, entity_id)

    async def get_or_raise(self, entity_id: Any) -> ModelT:
        entity = await self.get(entity_id)
        if entity is None:
            raise NotFoundError(f"{self.model.__name__} '{entity_id}' was not found")
        return entity

    async def page(self, offset: int = 0, limit: int = 100) -> Sequence[ModelT]:
        return await self._scalars(self._ordered_select().offset(offset).limit(limit))

    def add(self, entity: ModelT) -> None:
        self._session.add(entity)

    async def delete(self, entity: ModelT) -> None:
        await self._session.delete(entity)

    async def flush(self) -> None:
        await self._session.flush()

    def _ordered_select(self) -> Select[tuple[ModelT]]:
        return select(self.model).order_by(*self.model.__mapper__.primary_key)

    async def _scalars(self, statement: Select[tuple[ModelT]]) -> Sequence[ModelT]:
        result = await self._session.scalars(statement)
        return result.all()
