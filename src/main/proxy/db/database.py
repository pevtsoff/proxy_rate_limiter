from sqlalchemy.ext.asyncio import (
    AsyncConnection,
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)


def create_engine(database_url: str) -> AsyncEngine:
    return create_async_engine(database_url, pool_pre_ping=True)


def create_session_factory(bind: AsyncEngine | AsyncConnection) -> async_sessionmaker[AsyncSession]:
    """Sessions keep loaded attributes after commit so responses can be built from them."""
    return async_sessionmaker(bind=bind, expire_on_commit=False)
