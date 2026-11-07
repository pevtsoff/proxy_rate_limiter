from typing import Self

from pydantic import BaseModel, model_validator

from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.value_objects.assignment_target import AssignmentTarget
from proxy.schemas.field_types import PositiveId


class AssignmentUpdate(BaseModel):
    """Change the policy and/or the target. A new target always names its relationship type."""

    policy_id: PositiveId | None = None
    relationship_type: RelationshipType | None = None
    resource_id: PositiveId | None = None
    resource_group_id: PositiveId | None = None

    @model_validator(mode="after")
    def validate_target(self) -> Self:
        if self.relationship_type is not None:
            self.target()
            return self
        if self.resource_id is not None or self.resource_group_id is not None:
            raise ValueError("relationship_type is required when changing the target")
        return self

    def changes_target(self) -> bool:
        return self.relationship_type is not None

    def target(self) -> AssignmentTarget:
        if self.relationship_type is None:
            raise ValueError("relationship_type is required when changing the target")
        return AssignmentTarget(self.relationship_type, self.resource_id, self.resource_group_id)
