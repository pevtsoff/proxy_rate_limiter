"""Idempotently populate a small demo consumer and policy."""

import asyncio

from sqlalchemy import select

from app.database import engine, session_factory
from app.models.base import Base
from app.models.entities import Consumer, ConsumerGroup, ConsumerGroupPolicy, Policy, ResourceGroup


async def seed() -> None:
    """Create a regular demo consumer, group, and group-wide policy."""
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    async with session_factory() as session:
        group = await get_or_create_group(session)
        policy = await get_or_create_policy(session)
        await ensure_consumer(session, group.id)
        await ensure_assignment(session, group.id, policy.id)
        await session.commit()
    await engine.dispose()


async def get_or_create_group(session) -> ConsumerGroup:
    group = await session.scalar(select(ConsumerGroup).where(ConsumerGroup.group_name == "regular"))
    if group is None:
        group = ConsumerGroup(group_name="regular")
        session.add(group)
        await session.flush()
    return group


async def get_or_create_policy(session) -> Policy:
    policy = await session.scalar(select(Policy).where(Policy.name == "free-demo"))
    if policy is None:
        policy = Policy(name="free-demo", number_of_requests=5, time_period="minute")
        session.add(policy)
        await session.flush()
    return policy


async def ensure_consumer(session, group_id: int) -> None:
    if await session.get(Consumer, "demo-consumer") is None:
        session.add(Consumer(id="demo-consumer", consumer_group_id=group_id))


async def ensure_assignment(session, group_id: int, policy_id: int) -> None:
    resource_group = await session.scalar(select(ResourceGroup).where(ResourceGroup.name == "target-api"))
    if resource_group is None:
        return
    assignment = await session.scalar(select(ConsumerGroupPolicy).where(
        ConsumerGroupPolicy.consumer_group_id == group_id,
        ConsumerGroupPolicy.relationship_type == "resource_group",
        ConsumerGroupPolicy.resource_id == resource_group.id,
    ))
    if assignment is None:
        session.add(ConsumerGroupPolicy(consumer_group_id=group_id, relationship_type="resource_group", policy_id=policy_id, resource_id=resource_group.id))


if __name__ == "__main__":
    asyncio.run(seed())
