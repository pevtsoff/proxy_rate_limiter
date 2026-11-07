from pydantic import BaseModel

from proxy.domain.enums.io_type import IoType
from proxy.schemas.field_types import PositiveId


class ResourceUpdate(BaseModel):
    """Send `resource_group_id: null` explicitly to remove a resource from its group."""

    io_type: IoType | None = None
    resource_group_id: PositiveId | None = None
