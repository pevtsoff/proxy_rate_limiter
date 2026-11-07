from collections.abc import Sequence

from sqlalchemy import select

from proxy.db.models.resource import Resource
from proxy.domain.enums.http_method import HttpMethod
from proxy.repositories.sqlalchemy_repository import SqlAlchemyRepository


class ResourceRepository(SqlAlchemyRepository[Resource]):
    model = Resource

    async def list_by_method(self, method: HttpMethod) -> Sequence[Resource]:
        return await self._scalars(self._ordered_select().where(Resource.method == method))

    async def find(self, method: HttpMethod, resource_path: str) -> Resource | None:
        statement = select(Resource).where(
            Resource.method == method, Resource.resource_path == resource_path
        )
        return await self._session.scalar(statement)

    async def existing_keys(self) -> set[tuple[HttpMethod, str]]:
        result = await self._session.execute(select(Resource.method, Resource.resource_path))
        return {(HttpMethod(method), path) for method, path in result.all()}

    async def page_filtered(
        self, resource_group_id: int | None, offset: int, limit: int
    ) -> Sequence[Resource]:
        statement = self._ordered_select().offset(offset).limit(limit)
        if resource_group_id is not None:
            statement = statement.where(Resource.resource_group_id == resource_group_id)
        return await self._scalars(statement)
