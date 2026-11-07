from pydantic import BaseModel


class ResourceGroupRead(BaseModel):
    id: int
    name: str
    description: str | None
    resource_ids: list[int]
