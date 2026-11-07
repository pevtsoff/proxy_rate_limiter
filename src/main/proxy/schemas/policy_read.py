from pydantic import BaseModel, ConfigDict

from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod


class PolicyRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    rate_limiting_strategy: RateLimitingStrategy
    number_of_requests: int
    time_period: TimePeriod
    is_default: bool
