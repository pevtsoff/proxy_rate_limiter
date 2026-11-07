"""Database model declaration."""

from __future__ import annotations

from limits.strategies import STRATEGIES
from sqlalchemy import CheckConstraint, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Policy(Base):
    __tablename__ = "policies"
    __table_args__ = (
        CheckConstraint("number_of_requests > 0"),
        CheckConstraint("time_period IN ('second', 'minute', 'hour', 'day', 'week')"),
        CheckConstraint(
            "rate_limiting_strategy IN (" + ", ".join(f"'{name}'" for name in STRATEGIES) + ")"
        ),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True)
    rate_limiting_strategy: Mapped[str] = mapped_column(String(40), default="moving-window")
    number_of_requests: Mapped[int] = mapped_column(Integer)
    time_period: Mapped[str] = mapped_column(String(20))
