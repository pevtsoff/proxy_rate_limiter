"""Consumer-facing proxy acceptance coverage."""

import pytest
import uuid
from httpx import AsyncClient
from app.config import settings
from app.database import session_factory
from app.models.entities import Consumer, ConsumerGroup
from test_helpers import get_unassigned_consumer_group

@pytest.mark.asyncio
async def test_cuc1_missing_bearer_is_json(api_client: AsyncClient) -> None:
    """Requests without consumer identity are rejected consistently."""
    response = await api_client.get("/orders")
    assert response.status_code == 401
    assert response.json()["https_status_code"] == 401


@pytest.mark.asyncio
async def test_cuc1_exhausted_consumer_gets_json_429(
    api_client: AsyncClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    """Proxy returns the required JSON 429 after a consumer's capacity is used."""
    requests_seen = 0

    async def allow_once(key: str, limit: int, period: int) -> tuple[bool, int]:
        nonlocal requests_seen
        requests_seen += 1
        return requests_seen <= 1, max(0, 1 - requests_seen)

    async def resolve(*args: object, **kwargs: object) -> dict[str, int | str]:
        return {"limit": 1, "period": 60, "strategy": "moving-window"}

    monkeypatch.setattr("app.main.consume_capacity", allow_once)
    monkeypatch.setattr("app.main.resolve_policy", resolve)

    first = await api_client.get("/orders", headers={"Authorization": "Bearer cuc1-consumer"})
    blocked = await api_client.get("/orders", headers={"Authorization": "Bearer cuc1-consumer"})

    assert first.status_code == 200
    assert blocked.status_code == 429
    assert blocked.json() == {
        "error": "You exceeded the request capacity for the current period",
        "resource": "/orders",
        "https_status_code": 429,
    }


@pytest.mark.asyncio
async def test_cuc1_real_redis_window_blocks_the_next_request(
    api_client: AsyncClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    """A registered consumer is forwarded once and then limited by shared Redis."""
    consumer_id = f"cuc1-{uuid.uuid4().hex}"
    created_group = False
    async with session_factory() as session:
        group, created_group = await get_unassigned_consumer_group(session)
        session.add(Consumer(id=consumer_id, consumer_group_id=group.id))
        await session.commit()
    monkeypatch.setattr(settings, "default_requests", 1)

    try:
        first = await api_client.get("/orders", headers={"Authorization": f"Bearer {consumer_id}"})
        second = await api_client.get("/orders", headers={"Authorization": f"Bearer {consumer_id}"})
        assert first.status_code == 200
        assert second.status_code == 429
        assert second.json()["resource"] == "/orders"
    finally:
        await remove_consumer(consumer_id, created_group)


async def remove_consumer(consumer_id: str, remove_group: bool) -> None:
    """Remove committed CUC fixture records after the end-to-end request."""
    async with session_factory() as session:
        consumer = await session.get(Consumer, consumer_id)
        group_id = consumer.consumer_group_id if consumer else None
        if consumer is not None:
            await session.delete(consumer)
        if remove_group and group_id is not None:
            group = await session.get(ConsumerGroup, group_id)
            if group is not None:
                await session.delete(group)
        await session.commit()
