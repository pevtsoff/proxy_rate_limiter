from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from slowapi import Limiter
from slowapi.util import get_remote_address

from proxy.admin_ui.admin_ui_mounter import mount_admin_ui
from proxy.api.admin_rate_limit import build_admin_rate_limit_gate
from proxy.api.app_container import AppContainer
from proxy.api.container_factory import build_container
from proxy.api.error_handlers import register_exception_handlers
from proxy.api.middleware.exception_logging_middleware import ExceptionLoggingMiddleware
from proxy.api.routers import (
    assignments,
    consumer_groups,
    consumers,
    health,
    policies,
    proxy,
    resource_groups,
    resources,
    tokens,
)
from proxy.config.logging_config import configure_logging
from proxy.config.settings import get_settings
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.seeding.default_policy_bootstrapper import DefaultPolicyBootstrapper

MANAGEMENT_ROUTERS = (
    policies.router,
    consumer_groups.router,
    consumers.router,
    resources.router,
    resource_groups.router,
    assignments.router,
    tokens.router,
    health.router,
)


@asynccontextmanager
async def _lifespan(app: FastAPI) -> AsyncIterator[None]:
    container: AppContainer = app.state.container
    async with container.session_factory() as session:
        await DefaultPolicyBootstrapper(SqlAlchemyUnitOfWork(session), container.settings).ensure()
    yield
    await container.aclose()


def _configure_admin_rate_limit(app: FastAPI, container: AppContainer) -> None:
    settings = container.settings
    limiter = Limiter(key_func=get_remote_address, enabled=settings.admin_rate_limit_enabled)
    app.state.limiter = limiter
    app.state.admin_rate_limit_gate = build_admin_rate_limit_gate(
        limiter, settings.admin_rate_limit
    )


def create_app(container: AppContainer | None = None) -> FastAPI:
    """Build the proxy. Routing order matters: the catch-all proxy route is added last."""
    resolved = container or build_container(get_settings())
    configure_logging(resolved.settings.log_level)
    app = FastAPI(
        title="Proxy Rate Limiter",
        lifespan=_lifespan,
        docs_url="/admin/docs",
        redoc_url=None,
        openapi_url="/admin/openapi.json",
    )
    app.state.container = resolved
    _configure_admin_rate_limit(app, resolved)
    register_exception_handlers(app)
    app.add_middleware(ExceptionLoggingMiddleware)
    for router in MANAGEMENT_ROUTERS:
        app.include_router(router)
    mount_admin_ui(app, resolved)
    app.include_router(proxy.router)
    return app
