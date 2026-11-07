import jwt
import pytest

from proxy.domain.errors.authentication_error import AuthenticationError
from proxy.infrastructure.jwt_token_codec import JwtTokenCodec

SECRET = "unit-test-secret-that-is-long-enough-for-hs256"
codec = JwtTokenCodec(SECRET, "HS256")


def test_round_trip_returns_the_consumer() -> None:
    assert codec.decode(codec.encode("alice", 60)).value == "alice"


def test_expired_token_is_rejected() -> None:
    with pytest.raises(AuthenticationError, match="expired"):
        codec.decode(codec.encode("alice", -10))


def test_token_signed_with_another_secret_is_rejected() -> None:
    foreign = JwtTokenCodec("another-secret-that-is-also-long-enough!!", "HS256")
    with pytest.raises(AuthenticationError, match="invalid"):
        codec.decode(foreign.encode("alice", 60))


def test_garbage_is_rejected() -> None:
    with pytest.raises(AuthenticationError):
        codec.decode("not-a-jwt")


def test_token_without_subject_is_rejected() -> None:
    token = jwt.encode({"exp": 9_999_999_999}, SECRET, algorithm="HS256")
    with pytest.raises(AuthenticationError):
        codec.decode(token)
