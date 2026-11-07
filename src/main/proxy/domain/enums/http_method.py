from enum import StrEnum

from proxy.domain.enums.io_type import IoType

_READ_METHODS = frozenset({"GET", "HEAD", "OPTIONS", "TRACE"})


class HttpMethod(StrEnum):
    """Every HTTP request method a resource can be exposed with."""

    GET = "GET"
    HEAD = "HEAD"
    OPTIONS = "OPTIONS"
    TRACE = "TRACE"
    POST = "POST"
    PUT = "PUT"
    PATCH = "PATCH"
    DELETE = "DELETE"
    CONNECT = "CONNECT"

    @property
    def default_io_type(self) -> IoType:
        """Safe methods read; everything else is treated as a write."""
        return IoType.READ if self.value in _READ_METHODS else IoType.WRITE

    @classmethod
    def is_supported(cls, method: str) -> bool:
        return method.upper() in cls.__members__
