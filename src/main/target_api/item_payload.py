from pydantic import BaseModel, ConfigDict


class ItemPayload(BaseModel):
    """Free-form body accepted by every write endpoint."""

    model_config = ConfigDict(extra="allow")

    name: str | None = None
