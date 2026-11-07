from pydantic import BaseModel, ConfigDict

from proxy.domain.enums.consumer_group_name import ConsumerGroupName


class ConsumerGroupRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    group_name: ConsumerGroupName
    consumer_ids: list[str]
