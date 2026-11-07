import httpx
from redis.asyncio import Redis

from proxy.api.app_container import AppContainer
from proxy.config.settings import Settings
from proxy.db.database import create_engine, create_session_factory
from proxy.domain.value_objects.lru_cache_config import LruCacheConfig
from proxy.infrastructure.jwt_token_codec import JwtTokenCodec
from proxy.infrastructure.limits_rate_limiter import LimitsRateLimiter
from proxy.infrastructure.rate_limit_storage_factory import create_rate_limit_storage
from proxy.infrastructure.redis_lru_policy_cache import RedisLruPolicyCache


def build_container(settings: Settings) -> AppContainer:
    """Wire production implementations from settings."""
    engine = create_engine(settings.database_url)
    redis = Redis.from_url(settings.redis_url, decode_responses=True)
    cache_config = LruCacheConfig(settings.lru_cache_capacity, settings.lru_cache_ttl)
    return AppContainer(
        settings=settings,
        session_factory=create_session_factory(engine),
        redis=redis,
        http_client=httpx.AsyncClient(
            base_url=settings.target_api_url, timeout=settings.upstream_timeout_seconds
        ),
        policy_cache=RedisLruPolicyCache(redis, cache_config),
        rate_limiter=LimitsRateLimiter(create_rate_limit_storage(settings.rate_limit_storage_url)),
        token_codec=JwtTokenCodec(settings.jwt_secret.get_secret_value(), settings.jwt_algorithm),
        engine=engine,
    )
