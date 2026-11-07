"""sqladmin hands hooks raw form data: related objects arrive as primary keys (strings)."""

from typing import Any


def related_id(value: Any) -> int | None:
    if value is None or value == "":
        return None
    if hasattr(value, "id"):
        return int(value.id)
    return int(value)


def related_ids(values: Any) -> list[str]:
    if not values:
        return []
    return [str(getattr(value, "id", value)) for value in values]
