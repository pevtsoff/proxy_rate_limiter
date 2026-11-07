from collections.abc import Sequence
from functools import cached_property

from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from proxy.domain.errors.conflict_error import ConflictError
from proxy.ports.cache_invalidator import CacheInvalidator
from proxy.repositories.assignment_repository import AssignmentRepository
from proxy.repositories.consumer_group_repository import ConsumerGroupRepository
from proxy.repositories.consumer_repository import ConsumerRepository
from proxy.repositories.policy_repository import PolicyRepository
from proxy.repositories.resource_group_repository import ResourceGroupRepository
from proxy.repositories.resource_repository import ResourceRepository


class SqlAlchemyUnitOfWork:
    """One transaction plus the repositories working inside it.

    Every successful commit notifies the invalidators: any admin change may alter which
    policy applies, so the policy cache must be dropped (MUC5 AC3).
    """

    def __init__(
        self, session: AsyncSession, invalidators: Sequence[CacheInvalidator] = ()
    ) -> None:
        self._session = session
        self._invalidators = tuple(invalidators)

    @cached_property
    def policies(self) -> PolicyRepository:
        return PolicyRepository(self._session)

    @cached_property
    def consumer_groups(self) -> ConsumerGroupRepository:
        return ConsumerGroupRepository(self._session)

    @cached_property
    def consumers(self) -> ConsumerRepository:
        return ConsumerRepository(self._session)

    @cached_property
    def resources(self) -> ResourceRepository:
        return ResourceRepository(self._session)

    @cached_property
    def resource_groups(self) -> ResourceGroupRepository:
        return ResourceGroupRepository(self._session)

    @cached_property
    def assignments(self) -> AssignmentRepository:
        return AssignmentRepository(self._session)

    async def commit(self) -> None:
        await self._commit_or_raise_conflict()
        for invalidator in self._invalidators:
            await invalidator.invalidate()

    async def refresh(self, entity: object) -> None:
        await self._session.refresh(entity)

    async def _commit_or_raise_conflict(self) -> None:
        try:
            await self._session.commit()
        except IntegrityError as error:
            await self._session.rollback()
            raise ConflictError("The change violates a data integrity constraint") from error
