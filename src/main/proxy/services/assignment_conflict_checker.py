from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.errors.conflict_error import ConflictError
from proxy.domain.value_objects.assignment_target import AssignmentTarget
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork


class AssignmentConflictChecker:
    """Enforces MUC1 AC1: for one consumer group a resource is covered either by a
    resource assignment or by its resource group's assignment, never both.

    `all_resources` assignments are a fallback with the lowest precedence, so they only
    conflict with another `all_resources` assignment of the same group.
    """

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow

    async def ensure_assignable(
        self, consumer_group_id: int, target: AssignmentTarget, exclude_id: int | None = None
    ) -> None:
        if target.relationship_type is RelationshipType.RESOURCE:
            await self._check_resource(consumer_group_id, target, exclude_id)
            return
        if target.relationship_type is RelationshipType.RESOURCE_GROUP:
            await self._check_resource_group(consumer_group_id, target, exclude_id)
            return
        await self._check_all_resources(consumer_group_id, exclude_id)

    async def ensure_resource_can_join_group(
        self, resource_id: int, resource_group_id: int | None
    ) -> None:
        """Moving a resource into a group must not create an overlap either."""
        if resource_group_id is None:
            return
        assignments = self._uow.assignments
        by_resource = await assignments.consumer_groups_assigning_resource(resource_id)
        by_group = await assignments.consumer_groups_assigning_resource_group(resource_group_id)
        overlapping = sorted(by_resource & by_group)
        if overlapping:
            raise ConflictError(
                f"Resource {resource_id} has its own policy for consumer groups {overlapping}, "
                f"which also assign resource group {resource_group_id}"
            )

    async def _check_resource(
        self, consumer_group_id: int, target: AssignmentTarget, exclude_id: int | None
    ) -> None:
        resource = await self._uow.resources.get_or_raise(target.resource_id)
        assignments = self._uow.assignments
        if await assignments.exists_for_resource(consumer_group_id, resource.id, exclude_id):
            raise ConflictError(
                f"Consumer group {consumer_group_id} already has a policy for resource "
                f"{resource.id}"
            )
        group_id = resource.resource_group_id
        if group_id is not None and await assignments.exists_for_resource_group(
            consumer_group_id, group_id, exclude_id
        ):
            raise ConflictError(
                f"Resource {resource.id} belongs to resource group {group_id}, which already "
                f"has a policy for consumer group {consumer_group_id}"
            )

    async def _check_resource_group(
        self, consumer_group_id: int, target: AssignmentTarget, exclude_id: int | None
    ) -> None:
        group = await self._uow.resource_groups.get_or_raise(target.resource_group_id)
        assignments = self._uow.assignments
        if await assignments.exists_for_resource_group(consumer_group_id, group.id, exclude_id):
            raise ConflictError(
                f"Consumer group {consumer_group_id} already has a policy for resource group "
                f"{group.id}"
            )
        if await assignments.exists_for_any_resource_of_group(
            consumer_group_id, group.id, exclude_id
        ):
            raise ConflictError(
                f"Resources of resource group {group.id} already have their own policies for "
                f"consumer group {consumer_group_id}"
            )

    async def _check_all_resources(self, consumer_group_id: int, exclude_id: int | None) -> None:
        if await self._uow.assignments.exists_for_all_resources(consumer_group_id, exclude_id):
            raise ConflictError(
                f"Consumer group {consumer_group_id} already has an all_resources policy"
            )
