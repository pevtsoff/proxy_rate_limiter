from typing import Any

from pydantic import BaseModel


class ItemRead(BaseModel):
    id: int
    resource: str
    data: dict[str, Any] = {}
    revision: int | None = None
