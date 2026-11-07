from pydantic import BaseModel

from proxy.schemas.field_types import ConsumerIdField, PositiveId


class ConsumerCreate(BaseModel):
    id: ConsumerIdField
    consumer_group_id: PositiveId
