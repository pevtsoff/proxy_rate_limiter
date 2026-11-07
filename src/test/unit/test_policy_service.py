"""Unit coverage for limiter windows and OpenAPI path resolution."""

import pytest
from app.models import Resource, ResourceGroup
from app.services import policy_service
from app.services.policy_service import PERIOD_SECONDS, consume_capacity, find_resource
from doubles.fake_redis import FakeRedis
from sqlalchemy.ext.asyncio import AsyncSession


def test_period_values_are_seconds() -> None:
    """Configured intervals map to their expected seconds."""
    assert PERIOD_SECONDS == {"second": 1, "minute": 60, "hour": 3600, "day": 86400, "week": 604800}


@pytest.mark.asyncio
async def test_find_resource_matches_openapi_path_parameters(db_session: AsyncSession) -> None:
    """Concrete paths resolve against discovered parameterized OpenAPI paths."""
    group = ResourceGroup(name="test-target")
    db_session.add(group)
    await db_session.flush()
    db_session.add(
        Resource(
            io_type="read",
            method="GET",
            resource_path="/test-orders/{item_id}",
            resource_group_id=group.id,
        )
    )
    await db_session.flush()

    result = await find_resource(db_session, "GET", "/test-orders/42")

    assert result is not None
    assert result.resource_path == "/test-orders/{item_id}"


@pytest.mark.asyncio
async def test_capacity_counter_rejects_requests_after_limit(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """Atomic counter results allow up to the policy limit."""
    fake_redis = FakeRedis()
    monkeypatch.setattr(policy_service, "redis_client", fake_redis)

    first = await consume_capacity("consumer:path", limit=2, period=60)
    second = await consume_capacity("consumer:path", limit=2, period=60)
    third = await consume_capacity("consumer:path", limit=2, period=60)

    assert first == (True, 1)
    assert second == (True, 0)
    assert third == (False, 0)
