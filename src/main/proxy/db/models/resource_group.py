from typing import TYPE_CHECKING

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from proxy.db.base import Base

if TYPE_CHECKING:
    from proxy.db.models.resource import Resource


class ResourceGroup(Base):
    """A named set of resources that can share one policy assignment."""

    __tablename__ = "resource_groups"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True)
    description: Mapped[str | None] = mapped_column(String(500))
    resources: Mapped[list["Resource"]] = relationship(
        back_populates="resource_group", lazy="selectin"
    )

    def __str__(self) -> str:
        return self.name
