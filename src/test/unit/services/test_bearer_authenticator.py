import pytest

from proxy.domain.errors.authentication_error import AuthenticationError
from proxy.services.proxying.bearer_authenticator import BearerAuthenticator
from support.stub_token_decoder import StubTokenDecoder

authenticator = BearerAuthenticator(StubTokenDecoder())


def test_extracts_consumer_from_bearer_header() -> None:
    assert authenticator.authenticate("Bearer valid:alice", "orders/1").value == "alice"


def test_scheme_is_case_insensitive() -> None:
    assert authenticator.authenticate("bearer valid:alice", "orders/1").value == "alice"


@pytest.mark.parametrize("header", [None, "", "Basic abc", "Bearer ", "Bearer bad-token"])
def test_rejects_missing_or_invalid_tokens_and_reports_resource(header: str | None) -> None:
    with pytest.raises(AuthenticationError) as error:
        authenticator.authenticate(header, "orders/1")
    assert error.value.resource == "orders/1"
