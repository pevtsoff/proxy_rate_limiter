"""Proxy limiter REST API and transparent target API forwarding."""

import logging
import traceback
from contextlib import asynccontextmanager
from typing import AsyncIterator

import httpx
from fastapi import Depends, FastAPI, Request
from fastapi.responses import JSONResponse, Response
from sqlalchemy import select
from sqladmin import Admin
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.admin_view import CacheInvalidatingModelView
from app.database import engine, get_session, redis_client, session_factory
from app.models.base import Base
from app.models.entities import Consumer, ConsumerGroup, ConsumerGroupPolicy, Policy, Resource, ResourceGroup, ServiceSetting
from app.routes.admin_api import router as admin_api_router
from app.services.policy_service import consume_capacity, invalidate_policy_cache, resolve_policy

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger(__name__)


async def initialize() -> None:
    """Create the local schema and default configuration."""
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    async with session_factory() as session:
        if await session.scalar(select(Policy.id).where(Policy.name == "default")) is None:
            session.add(Policy(name="default", number_of_requests=settings.default_requests, time_period=settings.default_period))
            await session.commit()


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    """Initialize backing services and close shared connections."""
    await initialize()
    yield
    await redis_client.aclose()
    await engine.dispose()


app = FastAPI(title="Distributed Rate Limiter", lifespan=lifespan)
app.include_router(admin_api_router)


class PolicyAdmin(CacheInvalidatingModelView, model=Policy):
    column_list = [Policy.id, Policy.name, Policy.number_of_requests, Policy.time_period, Policy.rate_limiting_strategy]


class ConsumerGroupAdmin(CacheInvalidatingModelView, model=ConsumerGroup):
    column_list = [ConsumerGroup.id, ConsumerGroup.group_name]


class ConsumerAdmin(CacheInvalidatingModelView, model=Consumer):
    column_list = [Consumer.id, Consumer.consumer_group_id]


class ResourceGroupAdmin(CacheInvalidatingModelView, model=ResourceGroup):
    column_list = [ResourceGroup.id, ResourceGroup.name]


class ResourceAdmin(CacheInvalidatingModelView, model=Resource):
    column_list = [Resource.id, Resource.method, Resource.resource_path, Resource.io_type]


class AssignmentAdmin(CacheInvalidatingModelView, model=ConsumerGroupPolicy):
    column_list = [ConsumerGroupPolicy.id, ConsumerGroupPolicy.consumer_group_id, ConsumerGroupPolicy.policy_id]


class SettingsAdmin(CacheInvalidatingModelView, model=ServiceSetting):
    column_list = [ServiceSetting.id, ServiceSetting.key, ServiceSetting.value]


admin = Admin(app, engine, base_url="/admin", title="Rate Limiter Admin")
for view in [PolicyAdmin, ConsumerGroupAdmin, ConsumerAdmin, ResourceGroupAdmin, ResourceAdmin, AssignmentAdmin, SettingsAdmin]:
    admin.add_view(view)


@app.get("/health")
async def health() -> dict[str, str]:
    """Report service health."""
    await redis_client.ping()
    return {"status": "ok"}


@app.post("/admin-api/resources/sync")
async def sync_resources(session: AsyncSession = Depends(get_session)) -> dict[str, int]:
    """Import target endpoints and methods from its OpenAPI document."""
    async with httpx.AsyncClient() as client:
        spec = (await client.get(f"{settings.target_api_url}/openapi.json")).raise_for_status()
        paths = spec.json().get("paths", {})
    count = 0
    group = await session.scalar(select(ResourceGroup).where(ResourceGroup.name == "target-api"))
    if group is None:
        group = ResourceGroup(name="target-api")
        session.add(group)
        await session.flush()
    for path, operations in paths.items():
        for method in operations:
            if method.upper() not in {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}:
                continue
            existing = await session.scalar(select(Resource).where(Resource.method == method.upper(), Resource.resource_path == path))
            if existing is None:
                session.add(Resource(io_type="read" if method.upper() == "GET" else "write", method=method.upper(), resource_path=path, resource_group_id=group.id))
                count += 1
    await session.commit()
    return {"resources_synced": count}


@app.get("/admin-api/assignments")
async def list_assignments(session: AsyncSession = Depends(get_session)) -> list[dict[str, int | str]]:
    """List policy assignments."""
    rows = (await session.scalars(select(ConsumerGroupPolicy))).all()
    return [{"id": r.id, "consumer_group_id": r.consumer_group_id, "relationship_type": r.relationship_type, "policy_id": r.policy_id, "resource_id": r.resource_id} for r in rows]


@app.middleware("http")
async def proxy_requests(request: Request, call_next) -> Response:
    """Enforce an applicable policy then forward arbitrary target requests."""
    if request.url.path.startswith(("/admin", "/health", "/openapi", "/docs")):
        return await call_next(request)
    try:
        consumer_id = request.headers.get("authorization", "").removeprefix("Bearer ").strip()
        if not consumer_id:
            return JSONResponse(status_code=401, content={"error": "Bearer consumer token is required", "resource": request.url.path, "https_status_code": 401})
        async with session_factory() as session:
            policy = await resolve_policy(session, consumer_id, request.url.path, request.method)
        allowed, remaining = await consume_capacity(f"{consumer_id}:{request.method}:{request.url.path}", int(policy["limit"]), int(policy["period"]))
        if not allowed:
            return JSONResponse(status_code=429, content={"error": "You exceeded the request capacity for the current period", "resource": request.url.path, "https_status_code": 429})
        async with httpx.AsyncClient() as client:
            upstream = await client.request(request.method, f"{settings.target_api_url}{request.url.path}", params=request.query_params, content=await request.body(), headers={k: v for k, v in request.headers.items() if k.lower() not in {"host", "authorization", "content-length"}})
        return Response(content=upstream.content, status_code=upstream.status_code, headers={k: v for k, v in upstream.headers.items() if k.lower() not in {"content-length", "transfer-encoding", "connection"}}, media_type=upstream.headers.get("content-type"))
    except Exception as error:
        logger.error("Proxy request failed module=%s function=%s line=%s traceback=%s", __name__, "proxy_requests", error.__traceback__.tb_lineno if error.__traceback__ else 0, traceback.format_exc())
        return JSONResponse(status_code=502, content={"error": "Proxy could not process the request", "resource": request.url.path, "https_status_code": 502})
