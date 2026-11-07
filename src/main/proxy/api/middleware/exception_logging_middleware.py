import logging

from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response

from proxy.api.error_handlers import error_response

logger = logging.getLogger(__name__)

INTERNAL_ERROR_MESSAGE = "Internal server error"


class ExceptionLoggingMiddleware(BaseHTTPMiddleware):
    """Last line of defence: logs unexpected exceptions with traceback, answers JSON 500."""

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        try:
            return await call_next(request)
        except Exception:
            logger.exception("Unhandled error on %s %s", request.method, request.url.path)
            return error_response(request, 500, INTERNAL_ERROR_MESSAGE)
