from typing import Annotated

from pydantic import BaseModel, Field, field_validator

from proxy.domain.enums.consumer_group_name import ConsumerGroupName
from proxy.schemas.field_types import ConsumerIdField


class ConsumerGroupCreate(BaseModel):
    """A group is created together with its first consumers: it may never be empty."""

    group_name: ConsumerGroupName
    consumer_ids: Annotated[list[ConsumerIdField], Field(min_length=1)]

    @field_validator("consumer_ids")
    @classmethod
    def reject_duplicates(cls, consumer_ids: list[str]) -> list[str]:
        if len(set(consumer_ids)) != len(consumer_ids):
            raise ValueError("consumer_ids must be unique")
        return consumer_ids
