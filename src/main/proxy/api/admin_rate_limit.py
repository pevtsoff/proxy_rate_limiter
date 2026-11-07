"""slowapi protection of the management API (one shared bucket per client IP)."""

from collections.abc import Awaitable, Callable

from fastapi import Request
from slowapi import Limiter

AdminRateLimitGate = Callable[..., Awaitable[None]]


def build_admin_rate_limit_gate(limiter: Limiter, limit: str) -> AdminRateLimitGate:
    @limiter.limit(limit)
    async def admin_api(request: Request) -> None:
        return None

    return admin_api


async def enforce_admin_rate_limit(request: Request) -> None:
    """Router-level dependency; raises slowapi's `RateLimitExceeded` when over the limit."""
    gate: AdminRateLimitGate = request.app.state.admin_rate_limit_gate
    await gate(request=request)
