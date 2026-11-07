import pytest

from proxy.domain.errors.domain_validation_error import DomainValidationError
from proxy.domain.value_objects.consumer_id import ConsumerId


@pytest.mark.parametrize("raw", ["", "   ", "x" * 129])
def test_invalid_consumer_ids_are_rejected(raw: str) -> None:
    with pytest.raises(DomainValidationError):
        ConsumerId(raw)


def test_consumer_id_prints_as_its_value() -> None:
    assert str(ConsumerId("alice")) == "alice"
