"""Database model declaration."""

from __future__ import annotations

from sqlalchemy import CheckConstraint, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class ConsumerGroupPolicy(Base):
    __tablename__ = "consumer_groups_policies"
    __table_args__ = (
        CheckConstraint("relationship_type IN ('resource', 'resource_group')"),
        UniqueConstraint("consumer_group_id", "relationship_type", "resource_id"),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    consumer_group_id: Mapped[int] = mapped_column(ForeignKey("consumer_groups.id"))
    relationship_type: Mapped[str] = mapped_column(String(20))
    policy_id: Mapped[int] = mapped_column(ForeignKey("policies.id"))
    resource_id: Mapped[int] = mapped_column(Integer)
