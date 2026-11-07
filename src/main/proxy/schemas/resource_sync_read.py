from pydantic import BaseModel, ConfigDict


class ResourceSyncRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    discovered: int
    created: int
