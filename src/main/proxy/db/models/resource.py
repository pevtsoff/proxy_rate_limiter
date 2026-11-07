from typing import TYPE_CHECKING

from sqlalchemy import ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from proxy.db.base import Base
from proxy.db.enum_column_type import enum_column_type
from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.enums.io_type import IoType
from proxy.domain.value_objects.path_template import PathTemplate

if TYPE_CHECKING:
    from proxy.db.models.resource_group import ResourceGroup


class Resource(Base):
    """One operation of the target API, e.g. `GET /orders/{item_id}`."""

    __tablename__ = "resources"
    __table_args__ = (UniqueConstraint("method", "resource_path"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    method: Mapped[HttpMethod] = mapped_column(enum_column_type(HttpMethod))
    resource_path: Mapped[str] = mapped_column(String(500))
    io_type: Mapped[IoType] = mapped_column(enum_column_type(IoType))
    resource_group_id: Mapped[int | None] = mapped_column(
        ForeignKey("resource_groups.id", ondelete="SET NULL"), index=True
    )
    resource_group: Mapped["ResourceGroup | None"] = relationship(
        back_populates="resources", lazy="selectin"
    )

    @property
    def path_template(self) -> PathTemplate:
        return PathTemplate(self.resource_path)

    def __str__(self) -> str:
        return f"{self.method} {self.resource_path}"
