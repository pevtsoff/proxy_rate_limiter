from enum import StrEnum


class IoType(StrEnum):
    """Whether a resource reads or mutates state on the target API."""

    READ = "read"
    WRITE = "write"
