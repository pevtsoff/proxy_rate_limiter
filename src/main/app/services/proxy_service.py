"""Request enforcement and forwarding for the target API."""

import logging
import traceback
from collections.abc import Awaitable, Callable
from typing import Any

import httpx
from app.config import settings
from fastapi import Request
from fastapi.responses import JSONResponse, Response
from sqlalchemy.ext.asyncio import AsyncSession

logger = logging.getLogger(__name__)
PolicyResolver = Callable[[AsyncSession, str, str, str], Awaitable[dict[str, int | str]]]
CapacityConsumer = Callable[[str, int, int], Awaitable[tuple[bool, int]]]


async def handle_proxy_request(
    request: Request,
    session_factory: Callable[[], Any],
    resolve_policy: PolicyResolver,
    consume_capacity: CapacityConsumer,
) -> Response:
    """Authenticate, enforce the selected policy, and pass the request upstream."""
    resource = request.url.path
    try:
        consumer_id = extract_consumer_id(request)
        if consumer_id is None:
            return error_response(401, "Bearer consumer token is required", resource)
        policy = await load_policy(session_factory, resolve_policy, consumer_id, request)
        allowed, _ = await consume_capacity(
            build_counter_key(consumer_id, request), int(policy["limit"]), int(policy["period"])
        )
        if not allowed:
            return error_response(
                429, "You exceeded the request capacity for the current period", resource
            )
        return await forward_request(request)
    except Exception as error:  # noqa: BLE001 - proxy boundary must return a JSON error
        log_proxy_error(error)
        return error_response(502, "Proxy could not process the request", resource)


async def load_policy(
    session_factory: Callable[[], Any],
    resolver: PolicyResolver,
    consumer_id: str,
    request: Request,
) -> dict[str, int | str]:
    async with session_factory() as session:
        return await resolver(session, consumer_id, request.url.path, request.method)


def extract_consumer_id(request: Request) -> str | None:
    token = request.headers.get("authorization", "")
    consumer_id = token.removeprefix("Bearer ").strip()
    return consumer_id or None


def build_counter_key(consumer_id: str, request: Request) -> str:
    return f"{consumer_id}:{request.method}:{request.url.path}"


async def forward_request(request: Request) -> Response:
    excluded_headers = {"host", "authorization", "content-length"}
    headers = {
        key: value for key, value in request.headers.items() if key.lower() not in excluded_headers
    }
    async with httpx.AsyncClient() as client:
        upstream = await client.request(
            request.method,
            f"{settings.target_api_url}{request.url.path}",
            params=request.query_params,
            content=await request.body(),
            headers=headers,
        )
    return Response(
        content=upstream.content,
        status_code=upstream.status_code,
        headers=filter_response_headers(upstream.headers),
        media_type=upstream.headers.get("content-type"),
    )


def filter_response_headers(headers: httpx.Headers) -> dict[str, str]:
    excluded = {"content-length", "transfer-encoding", "connection"}
    return {key: value for key, value in headers.items() if key.lower() not in excluded}


def error_response(status_code: int, message: str, resource: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={"error": message, "resource": resource, "https_status_code": status_code},
    )


def log_proxy_error(error: Exception) -> None:
    line_number = error.__traceback__.tb_lineno if error.__traceback__ else 0
    logger.error(
        "Proxy request failed module=%s function=%s line=%s traceback=%s",
        __name__,
        "handle_proxy_request",
        line_number,
        traceback.format_exc(),
    )
