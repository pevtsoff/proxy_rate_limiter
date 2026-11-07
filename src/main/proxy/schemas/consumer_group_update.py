from pydantic import BaseModel

from proxy.domain.enums.consumer_group_name import ConsumerGroupName


class ConsumerGroupUpdate(BaseModel):
    group_name: ConsumerGroupName
