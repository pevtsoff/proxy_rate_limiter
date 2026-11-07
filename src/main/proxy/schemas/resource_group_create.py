from pydantic import BaseModel

from proxy.schemas.field_types import DescriptionField, NameField, PositiveId


class ResourceGroupCreate(BaseModel):
    name: NameField
    description: DescriptionField | None = None
    resource_ids: list[PositiveId] = []
