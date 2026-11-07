"""Shared helper functions for integration tests."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.entities import ConsumerGroup, ConsumerGroupPolicy


async def get_unassigned_consumer_group(session: AsyncSession) -> tuple[ConsumerGroup, bool]:
    """Return an unassigned group and whether this helper created it."""
    groups = (await session.scalars(select(ConsumerGroup))).all()
    for group in groups:
        assignment = await session.scalar(
            select(ConsumerGroupPolicy.id).where(
                ConsumerGroupPolicy.consumer_group_id == group.id
            )
        )
        if assignment is None:
            return group, False
    used_names = {group.group_name for group in groups}
    for name in ("premium", "regular", "free", "internal"):
        if name not in used_names:
            group = ConsumerGroup(group_name=name)
            session.add(group)
            await session.flush()
            return group, True
    raise AssertionError("Test database has no consumer group without policy assignments")
