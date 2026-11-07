import logging
import time

from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response

logger = logging.getLogger("target_api.requests")


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Logs every incoming request with its outcome."""

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        started = time.perf_counter()
        response = await call_next(request)
        logger.info(
            "%s %s query=%r consumer=%s client=%s status=%s duration_ms=%.1f",
            request.method,
            request.url.path,
            request.url.query,
            request.headers.get("x-consumer-id", "-"),
            request.client.host if request.client else "-",
            response.status_code,
            (time.perf_counter() - started) * 1000,
        )
        return response
