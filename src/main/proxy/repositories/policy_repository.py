from sqlalchemy import select

from proxy.db.models.policy import Policy
from proxy.repositories.sqlalchemy_repository import SqlAlchemyRepository


class PolicyRepository(SqlAlchemyRepository[Policy]):
    model = Policy

    async def get_default(self) -> Policy | None:
        return await self._session.scalar(select(Policy).where(Policy.is_default.is_(True)))

    async def get_by_name(self, name: str) -> Policy | None:
        return await self._session.scalar(select(Policy).where(Policy.name == name))
