from collections.abc import Sequence
from typing import Any

from sqlalchemy import ColumnElement, exists, func, select

from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource
from proxy.domain.enums.relationship_type import RelationshipType
from proxy.repositories.sqlalchemy_repository import SqlAlchemyRepository

Assignment = ConsumerGroupPolicy


class AssignmentRepository(SqlAlchemyRepository[ConsumerGroupPolicy]):
    """Queries over `consumer_groups_policies` (policy assignments)."""

    model = ConsumerGroupPolicy

    async def list_for_consumer_group(self, consumer_group_id: int) -> Sequence[Assignment]:
        statement = self._ordered_select().where(Assignment.consumer_group_id == consumer_group_id)
        return await self._scalars(statement)

    async def page_filtered(
        self, consumer_group_id: int | None, offset: int, limit: int
    ) -> Sequence[Assignment]:
        statement = self._ordered_select().offset(offset).limit(limit)
        if consumer_group_id is not None:
            statement = statement.where(Assignment.consumer_group_id == consumer_group_id)
        return await self._scalars(statement)

    async def exists_for_resource(
        self, consumer_group_id: int, resource_id: int, exclude_id: int | None
    ) -> bool:
        return await self._exists(
            consumer_group_id, exclude_id, Assignment.resource_id == resource_id
        )

    async def exists_for_resource_group(
        self, consumer_group_id: int, resource_group_id: int, exclude_id: int | None
    ) -> bool:
        return await self._exists(
            consumer_group_id, exclude_id, Assignment.resource_group_id == resource_group_id
        )

    async def exists_for_all_resources(
        self, consumer_group_id: int, exclude_id: int | None
    ) -> bool:
        condition = Assignment.relationship_type == RelationshipType.ALL_RESOURCES
        return await self._exists(consumer_group_id, exclude_id, condition)

    async def exists_for_any_resource_of_group(
        self, consumer_group_id: int, resource_group_id: int, exclude_id: int | None
    ) -> bool:
        resources_of_group = select(Resource.id).where(
            Resource.resource_group_id == resource_group_id
        )
        condition = Assignment.resource_id.in_(resources_of_group)
        return await self._exists(consumer_group_id, exclude_id, condition)

    async def consumer_groups_assigning_resource(self, resource_id: int) -> set[int]:
        return await self._consumer_group_ids(Assignment.resource_id == resource_id)

    async def consumer_groups_assigning_resource_group(self, resource_group_id: int) -> set[int]:
        return await self._consumer_group_ids(Assignment.resource_group_id == resource_group_id)

    async def count_using_policy(self, policy_id: int) -> int:
        statement = select(func.count()).where(Assignment.policy_id == policy_id)
        return await self._session.scalar(statement) or 0

    async def _exists(
        self, consumer_group_id: int, exclude_id: int | None, condition: ColumnElement[Any]
    ) -> bool:
        criteria = [Assignment.consumer_group_id == consumer_group_id, condition]
        if exclude_id is not None:
            criteria.append(Assignment.id != exclude_id)
        return bool(await self._session.scalar(select(exists().where(*criteria))))

    async def _consumer_group_ids(self, condition: ColumnElement[Any]) -> set[int]:
        result = await self._session.scalars(select(Assignment.consumer_group_id).where(condition))
        return set(result.all())
