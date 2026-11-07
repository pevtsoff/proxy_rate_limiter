"""Application settings loaded from the environment."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration."""

    database_url: str = "postgresql+asyncpg://rate_limiter:rate_limiter_local@localhost:5432/rate_limiter"
    redis_url: str = "redis://localhost:6379/0"
    target_api_url: str = "http://localhost:8001"
    admin_secret: str = "local-admin"
    lru_cache_capacity: int = 1000
    lru_cache_ttl: int = 3600
    default_requests: int = 100
    default_period: str = "minute"
    alembic_file_template: str = "%(year)d%(month).2d%(day).2d_%(hour).2d%(minute).2d%(second).2d_%%(slug)s"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
