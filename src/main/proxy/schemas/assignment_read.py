from pydantic import BaseModel, ConfigDict

from proxy.domain.enums.relationship_type import RelationshipType


class AssignmentRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    consumer_group_id: int
    relationship_type: RelationshipType
    policy_id: int
    resource_id: int | None
    resource_group_id: int | None
