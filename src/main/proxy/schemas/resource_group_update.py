from pydantic import BaseModel

from proxy.schemas.field_types import DescriptionField, NameField, PositiveId


class ResourceGroupUpdate(BaseModel):
    """`resource_ids`, when given, replaces the group's membership."""

    name: NameField | None = None
    description: DescriptionField | None = None
    resource_ids: list[PositiveId] | None = None
