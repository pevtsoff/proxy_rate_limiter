"""Shared asynchronous database fixtures for isolated tests."""

from collections.abc import AsyncIterator

import pytest_asyncio
from app.database import engine, get_session
from app.main import app
from app.models.base import Base
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession


@pytest_asyncio.fixture
async def db_session() -> AsyncIterator[AsyncSession]:
    """Provide a session whose writes are rolled back after each test."""
    async with engine.begin() as schema_connection:
        await schema_connection.run_sync(Base.metadata.create_all)
    async with engine.connect() as connection:
        transaction = await connection.begin()
        session = AsyncSession(
            bind=connection,
            expire_on_commit=False,
            join_transaction_mode="create_savepoint",
        )
        try:
            yield session
        finally:
            await session.close()
            await transaction.rollback()


@pytest_asyncio.fixture
async def api_client(db_session: AsyncSession) -> AsyncIterator[AsyncClient]:
    """Provide an async client whose endpoint database writes roll back."""

    async def override_session() -> AsyncIterator[AsyncSession]:
        yield db_session

    app.dependency_overrides[get_session] = override_session
    try:
        async with (
            app.router.lifespan_context(app),
            AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client,
        ):
            yield client
    finally:
        app.dependency_overrides.clear()
