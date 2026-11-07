from collections.abc import Sequence

from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.assignment_create import AssignmentCreate
from proxy.schemas.assignment_update import AssignmentUpdate
from proxy.services.assignment_conflict_checker import AssignmentConflictChecker


class AssignmentService:
    """Use cases MUC1.5 and MUC2: assigning policies to consumer groups."""

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow
        self._conflicts = AssignmentConflictChecker(uow)

    async def create(self, command: AssignmentCreate) -> ConsumerGroupPolicy:
        await self._uow.consumer_groups.get_or_raise(command.consumer_group_id)
        await self._uow.policies.get_or_raise(command.policy_id)
        target = command.target()
        await self._conflicts.ensure_assignable(command.consumer_group_id, target)
        assignment = ConsumerGroupPolicy(
            consumer_group_id=command.consumer_group_id, policy_id=command.policy_id
        )
        assignment.apply_target(target)
        self._uow.assignments.add(assignment)
        await self._uow.commit()
        return assignment

    async def page(
        self, consumer_group_id: int | None, offset: int, limit: int
    ) -> Sequence[ConsumerGroupPolicy]:
        return await self._uow.assignments.page_filtered(consumer_group_id, offset, limit)

    async def get(self, assignment_id: int) -> ConsumerGroupPolicy:
        return await self._uow.assignments.get_or_raise(assignment_id)

    async def update(self, assignment_id: int, command: AssignmentUpdate) -> ConsumerGroupPolicy:
        assignment = await self.get(assignment_id)
        if command.policy_id is not None:
            assignment.policy = await self._uow.policies.get_or_raise(command.policy_id)
        if command.changes_target():
            await self._retarget(assignment, command)
        await self._uow.commit()
        return assignment

    async def delete(self, assignment_id: int) -> None:
        assignment = await self.get(assignment_id)
        await self._uow.assignments.delete(assignment)
        await self._uow.commit()

    async def _retarget(self, assignment: ConsumerGroupPolicy, command: AssignmentUpdate) -> None:
        target = command.target()
        await self._conflicts.ensure_assignable(
            assignment.consumer_group_id, target, exclude_id=assignment.id
        )
        assignment.apply_target(target)
