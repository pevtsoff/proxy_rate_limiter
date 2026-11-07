"""Demo seed data setup and idempotency coverage."""

import pytest
from app.models import Consumer, ConsumerGroup, ConsumerGroupPolicy, ResourceGroup
from app.seed import GROUP_SEEDS, seed_groups
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession


@pytest.mark.asyncio
async def test_seed_creates_ten_groups_twenty_consumers_and_assignments(
    db_session: AsyncSession,
) -> None:
    """Seed covers every customer type with two consumers and one policy per group."""
    await ensure_target_resource_group(db_session)
    await seed_groups(db_session)
    await seed_groups(db_session)

    names = {group[0] for group in GROUP_SEEDS}
    groups = await count_matching_groups(db_session, names)
    consumers = await count_seeded_consumers(db_session, names)
    assignments = await count_group_assignments(db_session, names)
    group_types = await read_seeded_group_types(db_session, names)

    assert (groups, consumers, assignments) == (10, 20, 10)
    assert group_types == {"free", "regular", "premium", "internal"}


async def count_matching_groups(session: AsyncSession, names: set[str]) -> int:
    return (
        await session.scalar(
            select(func.count())
            .select_from(ConsumerGroup)
            .where(ConsumerGroup.group_name.in_(names))
        )
        or 0
    )


async def count_seeded_consumers(session: AsyncSession, names: set[str]) -> int:
    return (
        await session.scalar(
            select(func.count())
            .select_from(Consumer)
            .where(
                Consumer.id.like("%-consumer-%"),
                Consumer.consumer_group_id.in_(
                    select(ConsumerGroup.id).where(ConsumerGroup.group_name.in_(names))
                ),
            )
        )
        or 0
    )


async def count_group_assignments(session: AsyncSession, names: set[str]) -> int:
    return (
        await session.scalar(
            select(func.count())
            .select_from(ConsumerGroupPolicy)
            .where(
                ConsumerGroupPolicy.consumer_group_id.in_(
                    select(ConsumerGroup.id).where(ConsumerGroup.group_name.in_(names))
                )
            )
        )
        or 0
    )


async def read_seeded_group_types(session: AsyncSession, names: set[str]) -> set[str]:
    return set(
        (
            await session.scalars(
                select(ConsumerGroup.group_type).where(ConsumerGroup.group_name.in_(names))
            )
        ).all()
    )


async def ensure_target_resource_group(session: AsyncSession) -> None:
    group = await session.scalar(select(ResourceGroup).where(ResourceGroup.name == "target-api"))
    if group is None:
        session.add(ResourceGroup(name="target-api"))
        await session.flush()
