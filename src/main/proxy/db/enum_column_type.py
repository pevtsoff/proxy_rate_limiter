"""Enum columns are stored as VARCHAR + CHECK so adding a value needs no ALTER TYPE."""

import re
from enum import Enum

from sqlalchemy import Enum as SqlEnum

ENUM_COLUMN_LENGTH = 32


def _snake_case(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def _enum_values(enum_class: type[Enum]) -> list[str]:
    return [member.value for member in enum_class]


def enum_column_type(enum_class: type[Enum]) -> SqlEnum:
    """Column type persisting the enum *values* (e.g. `moving-window`), not member names."""
    return SqlEnum(
        enum_class,
        name=_snake_case(enum_class.__name__),
        native_enum=False,
        create_constraint=True,
        length=ENUM_COLUMN_LENGTH,
        values_callable=_enum_values,
        validate_strings=True,
    )
