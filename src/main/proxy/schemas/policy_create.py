from typing import Annotated

from pydantic import BaseModel, Field

from proxy.domain.enums.rate_limiting_strategy import DEFAULT_STRATEGY, RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.schemas.field_types import NameField


class PolicyCreate(BaseModel):
    name: NameField
    rate_limiting_strategy: RateLimitingStrategy = DEFAULT_STRATEGY
    number_of_requests: Annotated[int, Field(ge=1)]
    time_period: TimePeriod
