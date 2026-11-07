"""Database model declaration."""

from __future__ import annotations

from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base

if TYPE_CHECKING:
    from app.models.consumer import Consumer


class ConsumerGroup(Base):
    __tablename__ = "consumer_groups"
    __table_args__ = (CheckConstraint("group_name IN ('premium', 'regular', 'free', 'internal')"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    group_name: Mapped[str] = mapped_column(String(20), unique=True)
    consumers: Mapped[list[Consumer]] = relationship(back_populates="consumer_group")
