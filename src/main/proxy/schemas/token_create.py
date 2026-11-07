from typing import Annotated

from pydantic import BaseModel, Field

from proxy.schemas.field_types import ConsumerIdField

MAX_TOKEN_TTL_SECONDS = 31 * 86_400


class TokenCreate(BaseModel):
    consumer_id: ConsumerIdField
    ttl_seconds: Annotated[int, Field(ge=1, le=MAX_TOKEN_TTL_SECONDS)] | None = None
