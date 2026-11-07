import os

from fastapi import FastAPI

from target_api.logging_setup import configure_logging
from target_api.request_logging_middleware import RequestLoggingMiddleware
from target_api.resource_router_factory import build_resource_router

RESOURCES = ("orders", "users", "carts")


async def health() -> dict[str, str]:
    return {"status": "ok"}


def create_app() -> FastAPI:
    """The demo upstream API that sits behind the proxy rate limiter."""
    configure_logging(os.getenv("LOG_LEVEL", "INFO"))
    app = FastAPI(title="Target API", version="1.0.0")
    app.add_middleware(RequestLoggingMiddleware)
    for resource in RESOURCES:
        app.include_router(build_resource_router(resource))
    app.add_api_route("/health", health, include_in_schema=False)
    return app
