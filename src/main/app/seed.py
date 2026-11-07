"""Idempotently seed resource groups, consumers, policies, and assignments."""

import asyncio

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import engine, session_factory
from app.models import Consumer, ConsumerGroup, ConsumerGroupPolicy, Policy, Resource, ResourceGroup
from app.models.base import Base

GROUP_SEEDS = (
    ("free-entry", "free", "free-entry-limit", 5),
    ("free-limited", "free", "free-limited-limit", 2),
    ("regular-standard", "regular", "regular-standard-limit", 30),
    ("regular-volume", "regular", "regular-volume-limit", 60),
    ("regular-partner", "regular", "regular-partner-limit", 45),
    ("premium-standard", "premium", "premium-standard-limit", 120),
    ("premium-gold", "premium", "premium-gold-limit", 300),
    ("premium-batch", "premium", "premium-batch-limit", 200),
    ("internal-service", "internal", "internal-service-limit", 1000),
    ("internal-batch", "internal", "internal-batch-limit", 5000),
)
RESOURCE_GROUP_NAMES = ("orders", "users", "carts")


async def seed() -> None:
    """Ensure resource groups and tiered consumers with matching policy assignments."""
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    async with session_factory() as session:
        await seed_groups(session)
        await session.commit()
    await engine.dispose()


async def seed_groups(session: AsyncSession) -> None:
    resource_groups = await get_or_create_resource_groups(session)
    await assign_resources_to_groups(session, resource_groups)
    for group_name, group_type, policy_name, request_limit in GROUP_SEEDS:
        group = await get_or_create_group(session, group_name, group_type)
        policy = await get_or_create_policy(session, policy_name, request_limit)
        await ensure_group_consumers(session, group)
        for resource_group in resource_groups.values():
            await ensure_group_assignment(session, group, policy, resource_group)


async def get_or_create_resource_groups(session: AsyncSession) -> dict[str, ResourceGroup]:
    groups: dict[str, ResourceGroup] = {}
    for name in RESOURCE_GROUP_NAMES:
        group = await session.scalar(select(ResourceGroup).where(ResourceGroup.name == name))
        if group is None:
            group = ResourceGroup(name=name)
            session.add(group)
            await session.flush()
        groups[name] = group
    return groups


async def assign_resources_to_groups(
    session: AsyncSession, groups: dict[str, ResourceGroup]
) -> None:
    resources = (await session.scalars(select(Resource))).all()
    for resource in resources:
        group_name = resource.resource_path.strip("/").split("/", maxsplit=1)[0]
        group = groups.get(group_name)
        if group is not None:
            resource.resource_group_id = group.id


async def get_or_create_group(
    session: AsyncSession, group_name: str, group_type: str
) -> ConsumerGroup:
    group = await session.scalar(
        select(ConsumerGroup).where(ConsumerGroup.group_name == group_name)
    )
    if group is None:
        group = ConsumerGroup(group_name=group_name, group_type=group_type)
        session.add(group)
        await session.flush()
    else:
        group.group_type = group_type
    return group


async def get_or_create_policy(session: AsyncSession, name: str, request_limit: int) -> Policy:
    policy = await session.scalar(select(Policy).where(Policy.name == name))
    if policy is None:
        policy = Policy(name=name, number_of_requests=request_limit, time_period="minute")
        session.add(policy)
        await session.flush()
    else:
        policy.number_of_requests = request_limit
        policy.time_period = "minute"
    return policy


async def ensure_group_consumers(session: AsyncSession, group: ConsumerGroup) -> None:
    for sequence in (1, 2):
        consumer_id = f"{group.group_name}-consumer-{sequence:02d}"
        consumer = await session.get(Consumer, consumer_id)
        if consumer is None:
            session.add(Consumer(id=consumer_id, consumer_group_id=group.id))
        else:
            consumer.consumer_group_id = group.id


async def ensure_group_assignment(
    session: AsyncSession,
    group: ConsumerGroup,
    policy: Policy,
    resource_group: ResourceGroup,
) -> None:
    assignment = await session.scalar(
        select(ConsumerGroupPolicy).where(
            ConsumerGroupPolicy.consumer_group_id == group.id,
            ConsumerGroupPolicy.relationship_type == "resource_group",
            ConsumerGroupPolicy.resource_id == resource_group.id,
        )
    )
    if assignment is not None:
        assignment.policy_id = policy.id
        return
    session.add(
        ConsumerGroupPolicy(
            consumer_group_id=group.id,
            relationship_type="resource_group",
            policy_id=policy.id,
            resource_id=resource_group.id,
        )
    )


if __name__ == "__main__":
    asyncio.run(seed())
