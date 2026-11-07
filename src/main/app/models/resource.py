"""Database model declaration."""

from __future__ import annotations

from sqlalchemy import CheckConstraint, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Resource(Base):
    __tablename__ = "resources"
    __table_args__ = (
        UniqueConstraint("method", "resource_path"),
        CheckConstraint("io_type IN ('read', 'write')"),
        CheckConstraint("method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS')"),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    io_type: Mapped[str] = mapped_column(String(10))
    method: Mapped[str] = mapped_column(String(10))
    resource_path: Mapped[str] = mapped_column(String(300))
    resource_group_id: Mapped[int] = mapped_column(ForeignKey("resource_groups.id"))
