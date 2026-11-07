from pydantic import BaseModel

from proxy.schemas.field_types import PositiveId


class ConsumerUpdate(BaseModel):
    consumer_group_id: PositiveId
