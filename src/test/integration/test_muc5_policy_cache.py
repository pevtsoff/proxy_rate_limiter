"""Policy cache use and invalidation coverage."""

import uuid

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import redis_client
from app.services import policy_service
from app.models.entities import Consumer, ConsumerGroup, ConsumerGroupPolicy, Policy, Resource, ResourceGroup
from app.services.policy_service import invalidate_policy_cache, resolve_policy


@pytest.mark.asyncio
async def test_muc5_resolution_is_cached_and_invalidation_reloads(
    db_session: AsyncSession,
) -> None:
    """A repeated lookup uses cached values until policy mutations invalidate them."""
    consumer_id = f"cache-{uuid.uuid4().hex}"
    group = await existing_or_new_group(db_session)
    policy = Policy(name=f"cache-{uuid.uuid4().hex}", number_of_requests=4, time_period="minute")
    resource_group = ResourceGroup(name=f"cache-{uuid.uuid4().hex}")
    db_session.add_all([policy, resource_group])
    await db_session.flush()
    resource = Resource(io_type="read", method="GET", resource_path=f"/cache-{uuid.uuid4().hex}", resource_group_id=resource_group.id)
    db_session.add_all([
        resource,
        Consumer(id=consumer_id, consumer_group_id=group.id),
        ConsumerGroupPolicy(consumer_group_id=group.id, relationship_type="resource_group", policy_id=policy.id, resource_id=resource_group.id),
    ])
    await db_session.flush()

    first = await resolve_policy(db_session, consumer_id, resource.resource_path, "GET")
    cache_key = f"policy-cache:{consumer_id}:GET:{resource.resource_path}"
    assert await redis_client.exists(cache_key) == 1
    policy.number_of_requests = 9
    await db_session.flush()
    policy_service._cache.clear()  # Simulate a second proxy instance without local state.
    cached = await resolve_policy(db_session, consumer_id, resource.resource_path, "GET")
    await invalidate_policy_cache()
    assert await redis_client.exists(cache_key) == 0
    refreshed = await resolve_policy(db_session, consumer_id, resource.resource_path, "GET")

    assert first["limit"] == 4
    assert cached["limit"] == 4
    assert refreshed["limit"] == 9


async def existing_or_new_group(session: AsyncSession) -> ConsumerGroup:
    """Use a valid group for the rollback-isolated cache test."""
    group = await session.scalar(select(ConsumerGroup).limit(1))
    if group is None:
        group = ConsumerGroup(group_name="regular")
        session.add(group)
        await session.flush()
    return group
