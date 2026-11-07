from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, ForeignKey, Index, UniqueConstraint, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from proxy.db.base import Base
from proxy.db.enum_column_type import enum_column_type
from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.value_objects.assignment_target import AssignmentTarget

if TYPE_CHECKING:
    from proxy.db.models.consumer_group import ConsumerGroup
    from proxy.db.models.policy import Policy
    from proxy.db.models.resource import Resource
    from proxy.db.models.resource_group import ResourceGroup

TARGET_MATCHES_RELATIONSHIP_TYPE = (
    "(relationship_type = 'resource'"
    " AND resource_id IS NOT NULL AND resource_group_id IS NULL)"
    " OR (relationship_type = 'resource_group'"
    " AND resource_group_id IS NOT NULL AND resource_id IS NULL)"
    " OR (relationship_type = 'all_resources'"
    " AND resource_id IS NULL AND resource_group_id IS NULL)"
)


class ConsumerGroupPolicy(Base):
    """Assignment of a policy to a consumer group for a resource, a resource group or all.

    Two typed FK columns replace the spec's polymorphic `resource_id` so PostgreSQL can
    enforce referential integrity; the CHECK keeps exactly one target per type.
    Overlap between resource and resource-group assignments spans tables and is
    validated by `AssignmentConflictChecker`.
    """

    __tablename__ = "consumer_groups_policies"
    __table_args__ = (
        CheckConstraint(TARGET_MATCHES_RELATIONSHIP_TYPE, name="target_matches_relationship"),
        UniqueConstraint("consumer_group_id", "resource_id", name="uq_cgp_group_resource"),
        UniqueConstraint(
            "consumer_group_id", "resource_group_id", name="uq_cgp_group_resource_group"
        ),
        Index(
            "uq_cgp_group_all_resources",
            "consumer_group_id",
            unique=True,
            postgresql_where=text("relationship_type = 'all_resources'"),
            sqlite_where=text("relationship_type = 'all_resources'"),
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    consumer_group_id: Mapped[int] = mapped_column(
        ForeignKey("consumer_groups.id", ondelete="CASCADE"), index=True
    )
    relationship_type: Mapped[RelationshipType] = mapped_column(enum_column_type(RelationshipType))
    policy_id: Mapped[int] = mapped_column(ForeignKey("policies.id", ondelete="RESTRICT"))
    resource_id: Mapped[int | None] = mapped_column(ForeignKey("resources.id", ondelete="CASCADE"))
    resource_group_id: Mapped[int | None] = mapped_column(
        ForeignKey("resource_groups.id", ondelete="CASCADE")
    )

    consumer_group: Mapped["ConsumerGroup"] = relationship(lazy="selectin")
    policy: Mapped["Policy"] = relationship(lazy="selectin")
    resource: Mapped["Resource | None"] = relationship(lazy="selectin")
    resource_group: Mapped["ResourceGroup | None"] = relationship(lazy="selectin")

    @property
    def target(self) -> AssignmentTarget:
        return AssignmentTarget(
            RelationshipType(self.relationship_type), self.resource_id, self.resource_group_id
        )

    def apply_target(self, target: AssignmentTarget) -> None:
        self.relationship_type = target.relationship_type
        self.resource_id = target.resource_id
        self.resource_group_id = target.resource_group_id

    def scope_key(self) -> str:
        """Requests matched by the same assignment share one counter per consumer."""
        return f"assignment:{self.id}"

    def __str__(self) -> str:
        return f"#{self.id} {self.relationship_type}"
