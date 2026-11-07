"""Reusable annotated field types for request schemas."""

from typing import Annotated

from pydantic import Field, StringConstraints

ConsumerIdField = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=128)
]
NameField = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100)]
DescriptionField = Annotated[str, StringConstraints(max_length=500)]
PositiveId = Annotated[int, Field(ge=1)]
