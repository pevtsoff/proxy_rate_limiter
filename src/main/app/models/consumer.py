"""Database model declaration."""

from __future__ import annotations

from typing import TYPE_CHECKING

from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base

if TYPE_CHECKING:
    from app.models.consumer_group import ConsumerGroup


class Consumer(Base):
    __tablename__ = "consumers"
    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    consumer_group_id: Mapped[int] = mapped_column(ForeignKey("consumer_groups.id"))
    consumer_group: Mapped[ConsumerGroup] = relationship(back_populates="consumers")
