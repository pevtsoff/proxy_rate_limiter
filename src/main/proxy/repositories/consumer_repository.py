from collections.abc import Iterable, Sequence

from sqlalchemy import func, select

from proxy.db.models.consumer import Consumer
from proxy.repositories.sqlalchemy_repository import SqlAlchemyRepository


class ConsumerRepository(SqlAlchemyRepository[Consumer]):
    model = Consumer

    async def page_in_group(
        self, consumer_group_id: int | None, offset: int, limit: int
    ) -> Sequence[Consumer]:
        statement = self._ordered_select().offset(offset).limit(limit)
        if consumer_group_id is not None:
            statement = statement.where(Consumer.consumer_group_id == consumer_group_id)
        return await self._scalars(statement)

    async def count_in_group(self, consumer_group_id: int) -> int:
        statement = select(func.count()).where(Consumer.consumer_group_id == consumer_group_id)
        return await self._session.scalar(statement) or 0

    async def existing_ids(self, consumer_ids: Iterable[str]) -> set[str]:
        statement = select(Consumer.id).where(Consumer.id.in_(list(consumer_ids)))
        result = await self._session.scalars(statement)
        return set(result.all())
