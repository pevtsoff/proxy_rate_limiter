"""Every user facing error becomes `{"error", "resource", "https_status_code"}` JSON."""

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from starlette.exceptions import HTTPException

from proxy.api.rate_limit_headers import rate_limit_headers
from proxy.domain.errors.proxy_error import ProxyError
from proxy.domain.errors.rate_limit_exceeded_error import RateLimitExceededError
from proxy.schemas.error_response import ErrorResponse

logger = logging.getLogger(__name__)


def error_response(
    request: Request,
    status_code: int,
    message: str,
    resource: str | None = None,
    headers: dict[str, str] | None = None,
) -> JSONResponse:
    body = ErrorResponse(
        error=message,
        resource=resource if resource is not None else request.url.path.lstrip("/"),
        https_status_code=status_code,
    )
    return JSONResponse(body.model_dump(), status_code=status_code, headers=headers)


async def handle_proxy_error(request: Request, error: Exception) -> JSONResponse:
    assert isinstance(error, ProxyError)
    _log_proxy_error(request, error)
    headers = None
    if isinstance(error, RateLimitExceededError):
        headers = rate_limit_headers(error.decision)
        headers["Retry-After"] = str(error.decision.retry_after_seconds())
    return error_response(request, error.status_code, error.message, error.resource, headers)


async def handle_admin_rate_limit(request: Request, error: Exception) -> JSONResponse:
    assert isinstance(error, RateLimitExceeded)
    logger.info("Admin API rate limit hit on %s: %s", request.url.path, error.detail)
    message = f"Admin API rate limit exceeded: {error.detail}"
    return error_response(request, 429, message)


async def handle_validation_error(request: Request, error: Exception) -> JSONResponse:
    assert isinstance(error, RequestValidationError)
    logger.info("Invalid request to %s: %s", request.url.path, error.errors())
    return error_response(request, 422, _describe_validation_errors(error))


async def handle_http_exception(request: Request, error: Exception) -> JSONResponse:
    assert isinstance(error, HTTPException)
    logger.info("HTTP %s on %s: %s", error.status_code, request.url.path, error.detail)
    return error_response(request, error.status_code, str(error.detail), headers=error.headers)


def register_exception_handlers(app: FastAPI) -> None:
    app.add_exception_handler(ProxyError, handle_proxy_error)
    app.add_exception_handler(RateLimitExceeded, handle_admin_rate_limit)
    app.add_exception_handler(RequestValidationError, handle_validation_error)
    app.add_exception_handler(HTTPException, handle_http_exception)


def _describe_validation_errors(error: RequestValidationError) -> str:
    parts = []
    for detail in error.errors():
        location = ".".join(str(part) for part in detail.get("loc", ()))
        parts.append(f"{location}: {detail.get('msg', 'invalid value')}")
    return "; ".join(parts) or "Invalid request"


def _log_proxy_error(request: Request, error: ProxyError) -> None:
    """Rejections caused by quota are expected traffic; log them without a traceback."""
    if isinstance(error, RateLimitExceededError):
        logger.info("Rate limit exceeded for %s %s", request.method, request.url.path)
        return
    logger.warning(
        "%s on %s %s: %s",
        type(error).__name__,
        request.method,
        request.url.path,
        error.message,
        exc_info=error,
    )
