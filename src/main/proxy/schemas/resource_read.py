from pydantic import BaseModel, ConfigDict

from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.enums.io_type import IoType


class ResourceRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    method: HttpMethod
    resource_path: str
    io_type: IoType
    resource_group_id: int | None
