"""Async database and Redis lifecycle."""

from collections.abc import AsyncIterator

from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.config import settings

engine = create_async_engine(settings.database_url, pool_pre_ping=True)
session_factory = async_sessionmaker(engine, expire_on_commit=False)
redis_client = Redis.from_url(settings.redis_url, decode_responses=True)


async def get_session() -> AsyncIterator[AsyncSession]:
    """Yield a request-scoped database session."""
    async with session_factory() as session:
        yield session
