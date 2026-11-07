from typing import TYPE_CHECKING

from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from proxy.db.base import Base

if TYPE_CHECKING:
    from proxy.db.models.consumer_group import ConsumerGroup


class Consumer(Base):
    """An API consumer (user or service); always belongs to exactly one consumer group."""

    __tablename__ = "consumers"

    id: Mapped[str] = mapped_column(String(128), primary_key=True)
    consumer_group_id: Mapped[int] = mapped_column(
        ForeignKey("consumer_groups.id", ondelete="CASCADE"), index=True
    )
    consumer_group: Mapped["ConsumerGroup"] = relationship(
        back_populates="consumers", lazy="selectin"
    )

    def __str__(self) -> str:
        return self.id
