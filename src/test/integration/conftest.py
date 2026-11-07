"""Integration fixtures: real PostgreSQL and Redis from docker compose.

Every test runs inside one outer transaction that is rolled back afterwards; the
application's own commits only release SAVEPOINTs (`join_transaction_mode`).
"""

from collections.abc import AsyncIterator, Callable

import httpx
import pytest
from fastapi import FastAPI
from limits.aio.storage import Storage
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncEngine, AsyncSession, async_sessionmaker

import proxy.db.models  # noqa: F401  (registers every table on Base.metadata)
from proxy.api.app_container import AppContainer
from proxy.app_factory import create_app
from proxy.config.settings import Settings
from proxy.db.base import Base
from proxy.db.database import create_engine
from proxy.domain.value_objects.lru_cache_config import LruCacheConfig
from proxy.infrastructure.jwt_token_codec import JwtTokenCodec
from proxy.infrastructure.limits_rate_limiter import LimitsRateLimiter
from proxy.infrastructure.openapi_spec_source import OpenApiSpecSource
from proxy.infrastructure.rate_limit_storage_factory import create_rate_limit_storage
from proxy.infrastructure.redis_lru_policy_cache import RedisLruPolicyCache
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.seeding.database_seeder import DatabaseSeeder
from proxy.seeding.default_policy_bootstrapper import DefaultPolicyBootstrapper
from target_api.app_factory import create_app as create_target_app

AuthHeaders = Callable[[str], dict[str, str]]


@pytest.fixture(scope="session")
def settings() -> Settings:
    return Settings(admin_rate_limit_enabled=False)


@pytest.fixture(scope="session")
async def engine(settings: Settings) -> AsyncIterator[AsyncEngine]:
    engine = create_engine(settings.database_url)
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.drop_all)
        await connection.run_sync(Base.metadata.create_all)
    yield engine
    await engine.dispose()


@pytest.fixture
async def db_connection(engine: AsyncEngine) -> AsyncIterator[AsyncConnection]:
    async with engine.connect() as connection:
        transaction = await connection.begin()
        try:
            yield connection
        finally:
            await transaction.rollback()


@pytest.fixture
def session_factory(db_connection: AsyncConnection) -> async_sessionmaker[AsyncSession]:
    return async_sessionmaker(
        bind=db_connection, expire_on_commit=False, join_transaction_mode="create_savepoint"
    )


@pytest.fixture
async def redis_client(settings: Settings) -> AsyncIterator[Redis]:
    client = Redis.from_url(settings.redis_url, decode_responses=True)
    await client.flushdb()
    yield client
    await client.flushdb()
    await client.aclose()


@pytest.fixture
async def rate_limit_storage(settings: Settings) -> AsyncIterator[Storage]:
    storage = create_rate_limit_storage(settings.rate_limit_storage_url)
    await storage.reset()
    yield storage
    await storage.reset()


@pytest.fixture
def target_app() -> FastAPI:
    return create_target_app()


@pytest.fixture
async def container(
    settings: Settings,
    session_factory: async_sessionmaker[AsyncSession],
    redis_client: Redis,
    rate_limit_storage: Storage,
    target_app: FastAPI,
) -> AsyncIterator[AppContainer]:
    """Production wiring, except that the target API is called in-process."""
    http_client = httpx.AsyncClient(
        transport=httpx.ASGITransport(app=target_app), base_url="http://target-api"
    )
    container = AppContainer(
        settings=settings,
        session_factory=session_factory,
        redis=redis_client,
        http_client=http_client,
        policy_cache=RedisLruPolicyCache(
            redis_client, LruCacheConfig(settings.lru_cache_capacity, settings.lru_cache_ttl)
        ),
        rate_limiter=LimitsRateLimiter(rate_limit_storage),
        token_codec=JwtTokenCodec(settings.jwt_secret.get_secret_value(), settings.jwt_algorithm),
    )
    async with session_factory() as session:
        await DefaultPolicyBootstrapper(SqlAlchemyUnitOfWork(session), settings).ensure()
    yield container
    await http_client.aclose()


@pytest.fixture
def app(container: AppContainer) -> FastAPI:
    return create_app(container)


@pytest.fixture
async def client(app: FastAPI) -> AsyncIterator[httpx.AsyncClient]:
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://proxy") as client:
        yield client


@pytest.fixture
async def seeded(container: AppContainer) -> None:
    """The same seed script `./seed.sh` runs (`python -m proxy.seeding`)."""
    async with container.session_factory() as session:
        uow = SqlAlchemyUnitOfWork(session, [container.policy_cache])
        source = OpenApiSpecSource(container.http_client, container.settings.target_openapi_path)
        await DatabaseSeeder(uow, source, container.settings).seed()


@pytest.fixture
async def synced(client: httpx.AsyncClient) -> list[dict]:
    """Resources imported from the target API swagger document."""
    await client.post("/admin/resources/sync")
    return (await client.get("/admin/resources")).json()


@pytest.fixture
def auth_headers(container: AppContainer) -> AuthHeaders:
    def build(consumer_id: str) -> dict[str, str]:
        token = container.token_codec.encode(consumer_id, 3600)
        return {"Authorization": f"Bearer {token}"}

    return build
