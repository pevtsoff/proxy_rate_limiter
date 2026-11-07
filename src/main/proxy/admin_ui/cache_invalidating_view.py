from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import TYPE_CHECKING, Any, ClassVar

from sqladmin import ModelView
from starlette.requests import Request

from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork

if TYPE_CHECKING:
    from proxy.api.app_container import AppContainer


class CacheInvalidatingView(ModelView):
    """Base admin view: every change drops the policy cache, exactly like the REST API.

    `container` is bound per application by `bind_view` in the admin UI mounter.
    """

    container: ClassVar["AppContainer"]

    async def after_model_change(
        self, data: dict[str, Any], model: Any, is_created: bool, request: Request
    ) -> None:
        await self.container.policy_cache.invalidate()

    async def after_model_delete(self, model: Any, request: Request) -> None:
        await self.container.policy_cache.invalidate()

    @asynccontextmanager
    async def unit_of_work(self) -> AsyncIterator[SqlAlchemyUnitOfWork]:
        async with self.container.session_factory() as session:
            yield SqlAlchemyUnitOfWork(session)
