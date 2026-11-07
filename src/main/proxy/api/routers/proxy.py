from fastapi import APIRouter, Request
from fastapi.responses import Response

from proxy.api.dependencies import ProxyRequestHandlerDep
from proxy.api.proxy_http_adapter import to_proxy_request, to_response

PROXIED_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"]

router = APIRouter(tags=["proxy"])


@router.api_route("/{path:path}", methods=PROXIED_METHODS, include_in_schema=False)
async def proxy_request(request: Request, handler: ProxyRequestHandlerDep) -> Response:
    """Catch-all: every non-admin request is rate limited and forwarded (CUC1)."""
    outcome = await handler.handle(await to_proxy_request(request))
    return to_response(outcome)
