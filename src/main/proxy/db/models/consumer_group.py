from typing import TYPE_CHECKING

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from proxy.db.base import Base
from proxy.domain.enums.consumer_group_name import ConsumerGroupName

if TYPE_CHECKING:
    from proxy.db.models.consumer import Consumer


class ConsumerGroup(Base):
    """A uniquely named consumer group with policies assigned at group level.

    Invariant "a group has at least one consumer" is enforced by the services.
    """

    __tablename__ = "consumer_groups"

    id: Mapped[int] = mapped_column(primary_key=True)
    group_name: Mapped[ConsumerGroupName] = mapped_column(String(32), unique=True)
    consumers: Mapped[list["Consumer"]] = relationship(
        back_populates="consumer_group",
        cascade="all, delete-orphan",
        passive_deletes=True,
        lazy="selectin",
    )

    @property
    def consumer_ids(self) -> list[str]:
        return sorted(consumer.id for consumer in self.consumers)

    def __str__(self) -> str:
        return str(self.group_name)
