from collections.abc import Sequence

from proxy.db.models.resource import Resource
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.resource_update import ResourceUpdate
from proxy.services.assignment_conflict_checker import AssignmentConflictChecker


class ResourceService:
    """Managing the resources discovered from the target API."""

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow
        self._conflicts = AssignmentConflictChecker(uow)

    async def page(
        self, resource_group_id: int | None, offset: int, limit: int
    ) -> Sequence[Resource]:
        return await self._uow.resources.page_filtered(resource_group_id, offset, limit)

    async def get(self, resource_id: int) -> Resource:
        return await self._uow.resources.get_or_raise(resource_id)

    async def update(self, resource_id: int, command: ResourceUpdate) -> Resource:
        resource = await self.get(resource_id)
        if command.io_type is not None:
            resource.io_type = command.io_type
        if "resource_group_id" in command.model_fields_set:
            await self._move_to_group(resource, command.resource_group_id)
        await self._uow.commit()
        return resource

    async def delete(self, resource_id: int) -> None:
        resource = await self.get(resource_id)
        await self._uow.resources.delete(resource)
        await self._uow.commit()

    async def _move_to_group(self, resource: Resource, resource_group_id: int | None) -> None:
        if resource_group_id == resource.resource_group_id:
            return
        group = None
        if resource_group_id is not None:
            group = await self._uow.resource_groups.get_or_raise(resource_group_id)
        await self._conflicts.ensure_resource_can_join_group(resource.id, resource_group_id)
        resource.resource_group = group
