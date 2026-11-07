"""Policy cache use and invalidation coverage."""

import uuid

import pytest
from app.database import redis_client
from app.models import (
    Consumer,
    ConsumerGroup,
    ConsumerGroupPolicy,
    Policy,
    Resource,
    ResourceGroup,
)
from app.services import policy_service
from app.services.policy_service import invalidate_policy_cache, resolve_policy
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession


@pytest.mark.asyncio
async def test_muc5_resolution_is_cached_and_invalidation_reloads(
    db_session: AsyncSession,
) -> None:
    """A repeated lookup uses cached values until policy mutations invalidate them."""
    consumer_id, resource_path, policy = await cache_fixture_data(db_session)
    first = await resolve_policy(db_session, consumer_id, resource_path, "GET")
    cache_key = f"policy-cache:{consumer_id}:GET:{resource_path}"
    assert await redis_client.exists(cache_key) == 1
    cached, refreshed = await simulate_remote_cache_reload(
        db_session, consumer_id, resource_path, policy, cache_key
    )
    assert (first["limit"], cached["limit"], refreshed["limit"]) == (4, 4, 9)


async def cache_fixture_data(session: AsyncSession) -> tuple[str, str, Policy]:
    consumer_id = f"cache-{uuid.uuid4().hex}"
    group = await existing_or_new_group(session)
    policy = Policy(name=f"cache-{uuid.uuid4().hex}", number_of_requests=4, time_period="minute")
    resource_group = ResourceGroup(name=f"cache-{uuid.uuid4().hex}")
    session.add_all([policy, resource_group])
    await session.flush()
    resource = Resource(
        io_type="read",
        method="GET",
        resource_path=f"/cache-{uuid.uuid4().hex}",
        resource_group_id=resource_group.id,
    )
    session.add_all(
        [
            resource,
            Consumer(id=consumer_id, consumer_group_id=group.id),
            ConsumerGroupPolicy(
                consumer_group_id=group.id,
                relationship_type="resource_group",
                policy_id=policy.id,
                resource_id=resource_group.id,
            ),
        ]
    )
    await session.flush()
    return consumer_id, resource.resource_path, policy


async def simulate_remote_cache_reload(
    session: AsyncSession, consumer_id: str, path: str, policy: Policy, cache_key: str
) -> tuple[dict[str, int | str], dict[str, int | str]]:
    policy.number_of_requests = 9
    await session.flush()
    policy_service.clear_local_policy_cache()
    cached = await resolve_policy(session, consumer_id, path, "GET")
    await invalidate_policy_cache()
    assert await redis_client.exists(cache_key) == 0
    refreshed = await resolve_policy(session, consumer_id, path, "GET")
    return cached, refreshed


async def existing_or_new_group(session: AsyncSession) -> ConsumerGroup:
    """Use a valid group for the rollback-isolated cache test."""
    group = await session.scalar(select(ConsumerGroup).limit(1))
    if group is None:
        group = ConsumerGroup(group_name="regular")
        session.add(group)
        await session.flush()
    return group
