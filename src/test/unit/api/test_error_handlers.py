import logging

import httpx
import pytest
from fastapi import FastAPI
from pydantic import BaseModel

from proxy.api.error_handlers import register_exception_handlers
from proxy.api.middleware.exception_logging_middleware import ExceptionLoggingMiddleware
from proxy.domain.errors.conflict_error import ConflictError
from proxy.domain.errors.not_found_error import NotFoundError
from proxy.domain.errors.rate_limit_exceeded_error import RateLimitExceededError
from proxy.domain.value_objects.rate_limit_decision import RateLimitDecision


class Payload(BaseModel):
    count: int


def _app() -> FastAPI:
    app = FastAPI()
    register_exception_handlers(app)
    app.add_middleware(ExceptionLoggingMiddleware)

    @app.get("/missing")
    async def missing() -> None:
        raise NotFoundError("Thing 'x' was not found")

    @app.get("/conflict")
    async def conflict() -> None:
        raise ConflictError("Already exists")

    @app.get("/limited")
    async def limited() -> None:
        raise RateLimitExceededError("orders/1", RateLimitDecision(False, 10, 0, 9e9))

    @app.post("/validated")
    async def validated(payload: Payload) -> None:
        return None

    @app.get("/boom")
    async def boom() -> None:
        raise RuntimeError("unexpected")

    return app


@pytest.fixture
async def client() -> httpx.AsyncClient:
    transport = httpx.ASGITransport(app=_app(), raise_app_exceptions=False)
    return httpx.AsyncClient(transport=transport, base_url="http://test")


@pytest.mark.parametrize(
    ("path", "status", "message"),
    [
        ("/missing", 404, "Thing 'x' was not found"),
        ("/conflict", 409, "Already exists"),
        ("/limited", 429, "You exceeded the request capacity for the current period"),
        ("/boom", 500, "Internal server error"),
        ("/nowhere", 404, "Not Found"),
    ],
)
async def test_errors_share_one_json_shape(
    client: httpx.AsyncClient, path: str, status: int, message: str
) -> None:
    response = await client.get(path)
    expected_resource = "orders/1" if path == "/limited" else path.lstrip("/")
    assert response.status_code == status
    assert response.json() == {
        "error": message,
        "resource": expected_resource,
        "https_status_code": status,
    }


async def test_429_carries_retry_after_and_rate_limit_headers(client: httpx.AsyncClient) -> None:
    response = await client.get("/limited")
    assert int(response.headers["Retry-After"]) > 0
    assert response.headers["X-RateLimit-Limit"] == "10"
    assert response.headers["X-RateLimit-Remaining"] == "0"


async def test_validation_errors_are_json_422(client: httpx.AsyncClient) -> None:
    response = await client.post("/validated", json={"count": "many"})
    assert response.status_code == 422
    assert response.json()["https_status_code"] == 422
    assert "count" in response.json()["error"]


async def test_unhandled_errors_are_logged_with_location_and_traceback(
    client: httpx.AsyncClient, caplog: pytest.LogCaptureFixture
) -> None:
    with caplog.at_level(logging.ERROR):
        await client.get("/boom")
    record = next(r for r in caplog.records if r.levelno == logging.ERROR)
    assert record.exc_info is not None
    assert record.funcName == "dispatch"
    assert record.module == "exception_logging_middleware"
