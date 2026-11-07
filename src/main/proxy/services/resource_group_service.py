from collections.abc import Sequence

from proxy.db.models.resource import Resource
from proxy.db.models.resource_group import ResourceGroup
from proxy.domain.errors.conflict_error import ConflictError
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.resource_group_create import ResourceGroupCreate
from proxy.schemas.resource_group_update import ResourceGroupUpdate
from proxy.services.assignment_conflict_checker import AssignmentConflictChecker


class ResourceGroupService:
    """Managing resource groups and their membership."""

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow
        self._conflicts = AssignmentConflictChecker(uow)

    async def create(self, command: ResourceGroupCreate) -> ResourceGroup:
        await self._ensure_name_available(command.name)
        resources = await self._load_resources(command.resource_ids)
        group = ResourceGroup(name=command.name, description=command.description)
        group.resources = list(resources)
        self._uow.resource_groups.add(group)
        await self._uow.commit()
        return group

    async def page(self, offset: int, limit: int) -> Sequence[ResourceGroup]:
        return await self._uow.resource_groups.page(offset, limit)

    async def get(self, resource_group_id: int) -> ResourceGroup:
        return await self._uow.resource_groups.get_or_raise(resource_group_id)

    async def update(self, resource_group_id: int, command: ResourceGroupUpdate) -> ResourceGroup:
        group = await self.get(resource_group_id)
        if command.name is not None and command.name != group.name:
            await self._ensure_name_available(command.name)
            group.name = command.name
        if "description" in command.model_fields_set:
            group.description = command.description
        if command.resource_ids is not None:
            await self._replace_members(group, command.resource_ids)
        await self._uow.commit()
        return group

    async def delete(self, resource_group_id: int) -> None:
        """Members become ungrouped; assignments of the group are deleted."""
        group = await self.get(resource_group_id)
        await self._uow.resource_groups.delete(group)
        await self._uow.commit()

    async def _replace_members(self, group: ResourceGroup, resource_ids: list[int]) -> None:
        resources = await self._load_resources(resource_ids)
        current_ids = {resource.id for resource in group.resources}
        for resource in resources:
            if resource.id not in current_ids:
                await self._conflicts.ensure_resource_can_join_group(resource.id, group.id)
        group.resources = list(resources)

    async def _load_resources(self, resource_ids: list[int]) -> list[Resource]:
        return [await self._uow.resources.get_or_raise(rid) for rid in dict.fromkeys(resource_ids)]

    async def _ensure_name_available(self, name: str) -> None:
        if await self._uow.resource_groups.get_by_name(name) is not None:
            raise ConflictError(f"Resource group '{name}' already exists")
