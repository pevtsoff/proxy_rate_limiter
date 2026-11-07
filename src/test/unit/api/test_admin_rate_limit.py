import httpx
from fastapi import Depends, FastAPI
from slowapi import Limiter
from slowapi.util import get_remote_address

from proxy.api.admin_rate_limit import build_admin_rate_limit_gate, enforce_admin_rate_limit
from proxy.api.error_handlers import register_exception_handlers


def _app(limit: str, enabled: bool = True) -> FastAPI:
    app = FastAPI()
    limiter = Limiter(key_func=get_remote_address, enabled=enabled)
    app.state.limiter = limiter
    app.state.admin_rate_limit_gate = build_admin_rate_limit_gate(limiter, limit)
    register_exception_handlers(app)

    @app.get("/admin/thing", dependencies=[Depends(enforce_admin_rate_limit)])
    async def thing() -> dict[str, bool]:
        return {"ok": True}

    return app


async def _statuses(app: FastAPI, count: int) -> list[int]:
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        return [(await client.get("/admin/thing")).status_code for _ in range(count)]


async def test_slowapi_limits_the_management_api() -> None:
    assert await _statuses(_app("2/minute"), 3) == [200, 200, 429]


async def test_management_limit_can_be_disabled() -> None:
    assert await _statuses(_app("1/minute", enabled=False), 3) == [200, 200, 200]


async def test_management_429_uses_the_common_error_shape() -> None:
    transport = httpx.ASGITransport(app=_app("1/minute"))
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        await client.get("/admin/thing")
        body = (await client.get("/admin/thing")).json()
    assert body["https_status_code"] == 429
    assert body["resource"] == "admin/thing"
