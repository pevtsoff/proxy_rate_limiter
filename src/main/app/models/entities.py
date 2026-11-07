"""Rate limiting configuration database entities."""

from sqlalchemy import Boolean, CheckConstraint, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship
from limits.strategies import STRATEGIES

from app.models.base import Base


class Policy(Base):
    __tablename__ = "policies"
    __table_args__ = (
        CheckConstraint("number_of_requests > 0"),
        CheckConstraint("time_period IN ('second', 'minute', 'hour', 'day', 'week')"),
        CheckConstraint("rate_limiting_strategy IN (" + ", ".join(f"'{name}'" for name in STRATEGIES) + ")"),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True)
    rate_limiting_strategy: Mapped[str] = mapped_column(String(40), default="moving-window")
    number_of_requests: Mapped[int] = mapped_column(Integer)
    time_period: Mapped[str] = mapped_column(String(20))


class ResourceGroup(Base):
    __tablename__ = "resource_groups"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True)


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


class ConsumerGroup(Base):
    __tablename__ = "consumer_groups"
    __table_args__ = (CheckConstraint("group_name IN ('premium', 'regular', 'free', 'internal')"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    group_name: Mapped[str] = mapped_column(String(20), unique=True)
    consumers: Mapped[list["Consumer"]] = relationship(back_populates="consumer_group")


class Consumer(Base):
    __tablename__ = "consumers"
    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    consumer_group_id: Mapped[int] = mapped_column(ForeignKey("consumer_groups.id"))
    consumer_group: Mapped[ConsumerGroup] = relationship(back_populates="consumers")


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


class ServiceSetting(Base):
    __tablename__ = "service_settings"
    id: Mapped[int] = mapped_column(primary_key=True)
    key: Mapped[str] = mapped_column(String(100), unique=True)
    value: Mapped[str] = mapped_column(String(300))
