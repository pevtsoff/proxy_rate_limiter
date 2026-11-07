"""Validated input for policy creation and replacement."""

from pydantic import BaseModel, Field


class PolicyInput(BaseModel):
    """Policy settings supplied by API managers."""

    name: str = Field(min_length=1, max_length=100)
    rate_limiting_strategy: str = "moving-window"
    number_of_requests: int = Field(gt=0)
    time_period: str
