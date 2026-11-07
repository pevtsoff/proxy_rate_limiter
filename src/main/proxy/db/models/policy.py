from sqlalchemy import CheckConstraint, Index, String, false, text
from sqlalchemy.orm import Mapped, mapped_column

from proxy.db.base import Base
from proxy.db.enum_column_type import enum_column_type
from proxy.domain.enums.rate_limiting_strategy import DEFAULT_STRATEGY, RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.domain.value_objects.rate_limit_rule import RateLimitRule


class Policy(Base):
    """A named rate limit: `number_of_requests` per `time_period` using a strategy."""

    __tablename__ = "policies"
    __table_args__ = (
        CheckConstraint("number_of_requests > 0", name="positive_number_of_requests"),
        Index(
            "uq_policies_single_default",
            "is_default",
            unique=True,
            postgresql_where=text("is_default"),
            sqlite_where=text("is_default"),
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True)
    rate_limiting_strategy: Mapped[RateLimitingStrategy] = mapped_column(
        enum_column_type(RateLimitingStrategy),
        default=DEFAULT_STRATEGY,
        server_default=DEFAULT_STRATEGY.value,
    )
    number_of_requests: Mapped[int]
    time_period: Mapped[TimePeriod] = mapped_column(enum_column_type(TimePeriod))
    is_default: Mapped[bool] = mapped_column(default=False, server_default=false())

    def to_rule(self) -> RateLimitRule:
        return RateLimitRule(
            requests=self.number_of_requests,
            period=TimePeriod(self.time_period),
            strategy=RateLimitingStrategy(self.rate_limiting_strategy),
        )

    def __str__(self) -> str:
        return f"{self.name} ({self.number_of_requests}/{self.time_period})"
