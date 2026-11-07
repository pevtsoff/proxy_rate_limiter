from collections.abc import Sequence

from proxy.db.models.policy import Policy
from proxy.domain.errors.conflict_error import ConflictError
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.policy_create import PolicyCreate
from proxy.schemas.policy_update import PolicyUpdate


class PolicyService:
    """Use cases MUC1.3, MUC3 and MUC4: managing rate limiting policies."""

    def __init__(self, uow: SqlAlchemyUnitOfWork) -> None:
        self._uow = uow

    async def create(self, command: PolicyCreate) -> Policy:
        await self._ensure_name_available(command.name)
        policy = Policy(**command.model_dump())
        self._uow.policies.add(policy)
        await self._uow.commit()
        return policy

    async def page(self, offset: int, limit: int) -> Sequence[Policy]:
        return await self._uow.policies.page(offset, limit)

    async def get(self, policy_id: int) -> Policy:
        return await self._uow.policies.get_or_raise(policy_id)

    async def update(self, policy_id: int, command: PolicyUpdate) -> Policy:
        policy = await self.get(policy_id)
        changes = command.model_dump(exclude_unset=True, exclude_none=True)
        if "name" in changes and changes["name"] != policy.name:
            await self._ensure_name_available(changes["name"])
        for field, value in changes.items():
            setattr(policy, field, value)
        await self._uow.commit()
        return policy

    async def delete(self, policy_id: int) -> None:
        policy = await self.get(policy_id)
        await self._ensure_deletable(policy)
        await self._uow.policies.delete(policy)
        await self._uow.commit()

    async def _ensure_name_available(self, name: str) -> None:
        if await self._uow.policies.get_by_name(name) is not None:
            raise ConflictError(f"Policy '{name}' already exists")

    async def _ensure_deletable(self, policy: Policy) -> None:
        if policy.is_default:
            raise ConflictError("The default policy cannot be deleted")
        if await self._uow.assignments.count_using_policy(policy.id):
            raise ConflictError(f"Policy '{policy.name}' is still assigned to consumer groups")
