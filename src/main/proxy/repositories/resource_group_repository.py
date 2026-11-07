from sqlalchemy import select

from proxy.db.models.resource_group import ResourceGroup
from proxy.repositories.sqlalchemy_repository import SqlAlchemyRepository


class ResourceGroupRepository(SqlAlchemyRepository[ResourceGroup]):
    model = ResourceGroup

    async def get_by_name(self, name: str) -> ResourceGroup | None:
        return await self._session.scalar(select(ResourceGroup).where(ResourceGroup.name == name))
