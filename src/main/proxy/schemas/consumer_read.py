from pydantic import BaseModel, ConfigDict


class ConsumerRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    consumer_group_id: int
