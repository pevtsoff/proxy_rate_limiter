"""Manager entity creation and target resource discovery coverage."""

import pytest
from app.models import Consumer, ConsumerGroup, Resource
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession


@pytest.mark.asyncio
async def test_muc1_consumer_requires_a_group(
    db_session: AsyncSession, api_client: AsyncClient
) -> None:
    """Consumer-group creation and required group membership are represented."""
    group_name = await free_group_name(db_session)
    group = ConsumerGroup(group_name=group_name)
    db_session.add(group)
    await db_session.flush()
    consumer = Consumer(id="muc1-premium-consumer", consumer_group_id=group.id)
    db_session.add(consumer)
    await db_session.flush()

    stored = await db_session.get(Consumer, consumer.id)
    assert stored is not None
    assert stored.consumer_group_id == group.id
    assert not Consumer.__table__.c.consumer_group_id.nullable


async def free_group_name(session: AsyncSession) -> str:
    """Select an unused value from the consumer group enum."""
    names = set((await session.scalars(select(ConsumerGroup.group_name))).all())
    return next(name for name in ("premium", "regular", "free", "internal") if name not in names)


@pytest.mark.asyncio
async def test_muc1_sync_imports_target_openapi_operations(
    api_client: AsyncClient, db_session: AsyncSession
) -> None:
    """Resource sync discovers the target's GET and write operations."""
    response = await api_client.post("/admin-api/resources/sync")

    assert response.status_code == 200
    assert response.json()["resources_synced"] >= 0
    resources = (
        await db_session.scalars(select(Resource).where(Resource.resource_path == "/orders"))
    ).all()
    assert {resource.method for resource in resources} >= {"GET", "POST", "PUT", "PATCH"}
    assert all(
        resource.io_type == ("read" if resource.method == "GET" else "write")
        for resource in resources
    )
