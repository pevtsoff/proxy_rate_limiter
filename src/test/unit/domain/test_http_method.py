import pytest

from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.enums.io_type import IoType


@pytest.mark.parametrize("method", ["POST", "PUT", "PATCH", "DELETE"])
def test_mutating_methods_default_to_write(method: str) -> None:
    assert HttpMethod(method).default_io_type is IoType.WRITE


@pytest.mark.parametrize("method", ["GET", "HEAD", "OPTIONS"])
def test_safe_methods_default_to_read(method: str) -> None:
    assert HttpMethod(method).default_io_type is IoType.READ


def test_is_supported_ignores_case_and_rejects_openapi_keys() -> None:
    assert HttpMethod.is_supported("get")
    assert not HttpMethod.is_supported("parameters")
