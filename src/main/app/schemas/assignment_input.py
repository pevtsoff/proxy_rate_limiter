"""Validated input for consumer-group policy assignments."""

from pydantic import BaseModel


class AssignmentInput(BaseModel):
    """Policy and target scope selected by an API manager."""

    consumer_group_id: int
    relationship_type: str
    policy_id: int
    resource_id: int
