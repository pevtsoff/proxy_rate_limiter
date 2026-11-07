from collections.abc import Sequence

from proxy.db.models.consumer import Consumer
from proxy.domain.errors.conflict_error import ConflictError
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.consumer_create import ConsumerCreate
from proxy.schemas.consumer_update import ConsumerUpdate
from proxy.services.consumer_membership_guard import ConsumerMembershipGuard


class ConsumerService:
    """Use case MUC1.2: consumers always belong to exactly one consumer group."""

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow
        self._guard = ConsumerMembershipGuard(uow.consumers)

    async def create(self, command: ConsumerCreate) -> Consumer:
        if await self._uow.consumers.get(command.id) is not None:
            raise ConflictError(f"Consumer '{command.id}' already exists")
        await self._uow.consumer_groups.get_or_raise(command.consumer_group_id)
        consumer = Consumer(id=command.id, consumer_group_id=command.consumer_group_id)
        self._uow.consumers.add(consumer)
        await self._uow.commit()
        return consumer

    async def page(
        self, consumer_group_id: int | None, offset: int, limit: int
    ) -> Sequence[Consumer]:
        return await self._uow.consumers.page_in_group(consumer_group_id, offset, limit)

    async def get(self, consumer_id: str) -> Consumer:
        return await self._uow.consumers.get_or_raise(consumer_id)

    async def update(self, consumer_id: str, command: ConsumerUpdate) -> Consumer:
        consumer = await self.get(consumer_id)
        if command.consumer_group_id == consumer.consumer_group_id:
            return consumer
        new_group = await self._uow.consumer_groups.get_or_raise(command.consumer_group_id)
        await self._guard.ensure_can_leave_group(consumer)
        consumer.consumer_group = new_group
        await self._uow.commit()
        return consumer

    async def delete(self, consumer_id: str) -> None:
        consumer = await self.get(consumer_id)
        await self._guard.ensure_can_leave_group(consumer)
        await self._uow.consumers.delete(consumer)
        await self._uow.commit()
