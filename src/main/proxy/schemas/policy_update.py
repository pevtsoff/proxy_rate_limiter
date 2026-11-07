from typing import Annotated

from pydantic import BaseModel, Field

from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod
from proxy.schemas.field_types import NameField


class PolicyUpdate(BaseModel):
    """Partial update: only fields present in the request body are changed."""

    name: NameField | None = None
    rate_limiting_strategy: RateLimitingStrategy | None = None
    number_of_requests: Annotated[int, Field(ge=1)] | None = None
    time_period: TimePeriod | None = None
