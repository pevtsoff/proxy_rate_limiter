from sqlalchemy import select

from proxy.db.models.consumer_group import ConsumerGroup
from proxy.domain.enums.consumer_group_name import ConsumerGroupName
from proxy.repositories.sqlalchemy_repository import SqlAlchemyRepository


class ConsumerGroupRepository(SqlAlchemyRepository[ConsumerGroup]):
    model = ConsumerGroup

    async def get_by_name(self, group_name: ConsumerGroupName) -> ConsumerGroup | None:
        statement = select(ConsumerGroup).where(ConsumerGroup.group_name == group_name)
        return await self._session.scalar(statement)
