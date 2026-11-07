"""Application composition for the proxy and manager API."""

import asyncio
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Annotated

from alembic import command
from alembic.config import Config
from fastapi import Depends, FastAPI, Request
from fastapi.responses import Response
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.admin_views import configure_admin
from app.config import settings
from app.database import engine, get_session, redis_client, session_factory
from app.models import ConsumerGroupPolicy, Policy
from app.resource_discovery import sync_target_resources
from app.routes.admin_api import router as admin_api_router
from app.services.policy_service import consume_capacity, resolve_policy
from app.services.proxy_service import handle_proxy_request


async def initialize() -> None:
    """Apply migrations and ensure the fallback policy exists."""
    await asyncio.to_thread(command.upgrade, Config("alembic.ini"), "head")
    async with session_factory() as session:
        await ensure_default_policy(session)


async def ensure_default_policy(session: AsyncSession) -> None:
    default_exists = await session.scalar(select(Policy.id).where(Policy.name == "default"))
    if default_exists is not None:
        return
    session.add(
        Policy(
            name="default",
            number_of_requests=settings.default_requests,
            time_period=settings.default_period,
        )
    )
    await session.commit()


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    """Initialize backing services and release shared connections on shutdown."""
    await initialize()
    yield
    await redis_client.aclose()
    await engine.dispose()


app = FastAPI(title="Distributed Rate Limiter", lifespan=lifespan)
app.include_router(admin_api_router)
app.state.policy_resolver = resolve_policy
app.state.capacity_consumer = consume_capacity
app.state.session_factory = session_factory
admin = configure_admin(app, engine)
SessionDep = Annotated[AsyncSession, Depends(get_session)]


@app.get("/health")
async def health() -> dict[str, str]:
    """Report service health after checking Redis connectivity."""
    await redis_client.ping()
    return {"status": "ok"}


@app.post("/admin-api/resources/sync")
async def sync_resources(session: SessionDep) -> dict[str, int]:
    """Import target operations from its published OpenAPI document."""
    return {"resources_synced": await sync_target_resources(session)}


@app.get("/admin-api/assignments")
async def list_assignments(
    session: SessionDep,
) -> list[dict[str, int | str]]:
    """Return policy assignments in API response form."""
    rows = (await session.scalars(select(ConsumerGroupPolicy))).all()
    return [serialize_assignment(row) for row in rows]


def serialize_assignment(row: ConsumerGroupPolicy) -> dict[str, int | str]:
    return {
        "id": row.id,
        "consumer_group_id": row.consumer_group_id,
        "relationship_type": row.relationship_type,
        "policy_id": row.policy_id,
        "resource_id": row.resource_id,
    }


@app.middleware("http")
async def proxy_requests(request: Request, call_next: object) -> Response:
    """Enforce rate limits for target calls and leave management routes untouched."""
    if is_management_request(request):
        return await call_next(request)  # type: ignore[operator]
    return await handle_proxy_request(
        request,
        getattr(request.app.state, "session_factory", session_factory),
        getattr(request.app.state, "policy_resolver", resolve_policy),
        getattr(request.app.state, "capacity_consumer", consume_capacity),
    )


def is_management_request(request: Request) -> bool:
    return request.url.path.startswith(("/admin", "/health", "/openapi", "/docs"))
