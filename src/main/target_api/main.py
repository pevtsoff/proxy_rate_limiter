"""Example target REST API used to demonstrate proxy forwarding."""

import logging
from typing import Any

from fastapi import FastAPI, Request

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("target-api")
app = FastAPI(title="Example Target API")


async def respond(resource: str, request: Request) -> dict[str, Any]:
    """Log every incoming call and return a static diagnostic response."""
    logger.info("method=%s path=%s", request.method, request.url.path)
    return {
        "service": "target-api",
        "resource": resource,
        "method": request.method,
        "message": "request forwarded",
    }


for resource_name in ("orders", "users", "carts"):
    for verb in ("get", "post", "put", "patch"):

        async def endpoint(request: Request, resource: str = resource_name) -> dict[str, Any]:
            return await respond(resource, request)

        app.add_api_route(
            f"/{resource_name}", endpoint, methods=[verb.upper()], name=f"{verb}_{resource_name}"
        )
        app.add_api_route(
            f"/{resource_name}/{{item_id}}",
            endpoint,
            methods=[verb.upper()],
            name=f"{verb}_{resource_name}_item",
        )


@app.get("/health")
async def health() -> dict[str, str]:
    """Report target service health."""
    return {"status": "ok"}
