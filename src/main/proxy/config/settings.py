"""Application settings loaded from environment variables and the `.env` file."""

from functools import lru_cache

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

from proxy.domain.enums.rate_limiting_strategy import DEFAULT_STRATEGY, RateLimitingStrategy
from proxy.domain.enums.time_period import TimePeriod


class Settings(BaseSettings):
    """Typed configuration of the proxy rate limiter."""

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+asyncpg://proxy:proxy@localhost:5432/proxy"
    redis_url: str = "redis://localhost:6379/0"
    rate_limit_storage_url: str = "async+redis://localhost:6379/1"

    target_api_url: str = "http://localhost:8001"
    target_openapi_path: str = "/openapi.json"
    upstream_timeout_seconds: float = 10.0

    jwt_secret: SecretStr = SecretStr("change-me-in-env-file-please-32-bytes")
    jwt_algorithm: str = "HS256"
    jwt_default_ttl_seconds: int = 86_400

    lru_cache_capacity: int = Field(default=1000, ge=1)
    lru_cache_ttl: int = Field(default=3600, ge=1)

    default_policy_name: str = "default"
    default_policy_strategy: RateLimitingStrategy = DEFAULT_STRATEGY
    default_policy_requests: int = Field(default=60, ge=1)
    default_policy_period: TimePeriod = TimePeriod.MINUTE

    admin_rate_limit: str = "1000/minute"
    admin_rate_limit_enabled: bool = True

    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    """Return the process-wide settings instance."""
    return Settings()
