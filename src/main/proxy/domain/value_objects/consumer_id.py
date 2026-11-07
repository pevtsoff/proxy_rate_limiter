from dataclasses import dataclass

from proxy.domain.errors.domain_validation_error import DomainValidationError

MAX_CONSUMER_ID_LENGTH = 128


@dataclass(frozen=True, slots=True)
class ConsumerId:
    """Identifier of an API consumer, as carried in the bearer token `sub` claim."""

    value: str

    def __post_init__(self) -> None:
        if not self.value or not self.value.strip():
            raise DomainValidationError("Consumer id must not be empty")
        if len(self.value) > MAX_CONSUMER_ID_LENGTH:
            raise DomainValidationError(
                f"Consumer id must be at most {MAX_CONSUMER_ID_LENGTH} characters"
            )

    def __str__(self) -> str:
        return self.value
