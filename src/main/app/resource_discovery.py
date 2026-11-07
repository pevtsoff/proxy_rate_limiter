"""Import endpoint definitions from the configured target API OpenAPI document."""

import httpx
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.models import Resource, ResourceGroup

SUPPORTED_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}


async def sync_target_resources(session: AsyncSession) -> int:
    """Create missing target API resource rows and return the number added."""
    paths = await fetch_openapi_paths()
    group = await get_target_group(session)
    count = 0
    for path, operations in paths.items():
        count += await import_path_operations(session, group.id, path, operations)
    await session.commit()
    return count


async def fetch_openapi_paths() -> dict[str, dict[str, object]]:
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{settings.target_api_url}/openapi.json")
    response.raise_for_status()
    return response.json().get("paths", {})


async def get_target_group(session: AsyncSession) -> ResourceGroup:
    group = await session.scalar(select(ResourceGroup).where(ResourceGroup.name == "target-api"))
    if group is not None:
        return group
    group = ResourceGroup(name="target-api")
    session.add(group)
    await session.flush()
    return group


async def import_path_operations(
    session: AsyncSession, group_id: int, path: str, operations: dict[str, object]
) -> int:
    added = 0
    for method in operations:
        normalized_method = method.upper()
        if normalized_method in SUPPORTED_METHODS:
            added += await add_resource_if_missing(session, group_id, path, normalized_method)
    return added


async def add_resource_if_missing(
    session: AsyncSession, group_id: int, path: str, method: str
) -> int:
    existing = await session.scalar(
        select(Resource.id).where(Resource.method == method, Resource.resource_path == path)
    )
    if existing is not None:
        return 0
    io_type = "read" if method == "GET" else "write"
    session.add(
        Resource(io_type=io_type, method=method, resource_path=path, resource_group_id=group_id)
    )
    return 1
