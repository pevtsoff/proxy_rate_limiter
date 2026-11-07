from typing import Self

from pydantic import BaseModel, model_validator

from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.value_objects.assignment_target import AssignmentTarget
from proxy.schemas.field_types import PositiveId


class AssignmentCreate(BaseModel):
    consumer_group_id: PositiveId
    relationship_type: RelationshipType
    policy_id: PositiveId
    resource_id: PositiveId | None = None
    resource_group_id: PositiveId | None = None

    @model_validator(mode="after")
    def validate_target(self) -> Self:
        self.target()
        return self

    def target(self) -> AssignmentTarget:
        return AssignmentTarget(self.relationship_type, self.resource_id, self.resource_group_id)
