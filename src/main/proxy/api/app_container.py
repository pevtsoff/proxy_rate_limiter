from dataclasses import dataclass

import httpx
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker

from proxy.config.settings import Settings
from proxy.infrastructure.jwt_token_codec import JwtTokenCodec
from proxy.infrastructure.limits_rate_limiter import LimitsRateLimiter
from proxy.infrastructure.redis_lru_policy_cache import RedisLruPolicyCache


@dataclass
class AppContainer:
    """Composition root: long-lived, process-wide collaborators."""

    settings: Settings
    session_factory: async_sessionmaker[AsyncSession]
    redis: Redis
    http_client: httpx.AsyncClient
    policy_cache: RedisLruPolicyCache
    rate_limiter: LimitsRateLimiter
    token_codec: JwtTokenCodec
    engine: AsyncEngine | None = None

    async def aclose(self) -> None:
        await self.http_client.aclose()
        await self.redis.aclose()
        if self.engine is not None:
            await self.engine.dispose()
