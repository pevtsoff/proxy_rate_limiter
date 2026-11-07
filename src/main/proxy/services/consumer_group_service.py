from collections.abc import Sequence

from proxy.db.models.consumer import Consumer
from proxy.db.models.consumer_group import ConsumerGroup
from proxy.domain.enums.consumer_group_name import ConsumerGroupName
from proxy.domain.errors.conflict_error import ConflictError
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.consumer_group_create import ConsumerGroupCreate
from proxy.schemas.consumer_group_update import ConsumerGroupUpdate


class ConsumerGroupService:
    """Use case MUC1.1: managing consumer groups."""

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow

    async def create(self, command: ConsumerGroupCreate) -> ConsumerGroup:
        await self._ensure_name_available(command.group_name)
        await self._ensure_consumers_are_new(command.consumer_ids)
        consumers = [Consumer(id=consumer_id) for consumer_id in command.consumer_ids]
        group = ConsumerGroup(group_name=command.group_name, consumers=consumers)
        self._uow.consumer_groups.add(group)
        await self._uow.commit()
        return group

    async def page(self, offset: int, limit: int) -> Sequence[ConsumerGroup]:
        return await self._uow.consumer_groups.page(offset, limit)

    async def get(self, consumer_group_id: int) -> ConsumerGroup:
        return await self._uow.consumer_groups.get_or_raise(consumer_group_id)

    async def update(self, consumer_group_id: int, command: ConsumerGroupUpdate) -> ConsumerGroup:
        group = await self.get(consumer_group_id)
        if command.group_name != group.group_name:
            await self._ensure_name_available(command.group_name)
        group.group_name = command.group_name
        await self._uow.commit()
        return group

    async def delete(self, consumer_group_id: int) -> None:
        """Deletes the group with its consumers and assignments: consumers cannot be orphaned."""
        group = await self.get(consumer_group_id)
        await self._uow.consumer_groups.delete(group)
        await self._uow.commit()

    async def _ensure_name_available(self, group_name: ConsumerGroupName) -> None:
        if await self._uow.consumer_groups.get_by_name(group_name) is not None:
            raise ConflictError(f"Consumer group '{group_name}' already exists")

    async def _ensure_consumers_are_new(self, consumer_ids: list[str]) -> None:
        taken = await self._uow.consumers.existing_ids(consumer_ids)
        if taken:
            raise ConflictError(f"Consumers already exist: {', '.join(sorted(taken))}")
