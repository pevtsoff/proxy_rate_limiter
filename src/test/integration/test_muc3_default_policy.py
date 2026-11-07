"""Default policy behavior coverage."""

import uuid

import pytest
from app.config import settings
from app.models import Consumer
from app.services.policy_service import resolve_policy
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from test_helpers import get_unassigned_consumer_group


@pytest.mark.asyncio
async def test_muc3_default_policy_is_present_and_used_without_assignment(
    api_client: AsyncClient, db_session: AsyncSession
) -> None:
    """An existing but unassigned consumer falls back to configured defaults."""
    listing = await api_client.get("/admin-api/policies")
    assert any(policy["name"] == "default" for policy in listing.json())
    group, _ = await get_unassigned_consumer_group(db_session)
    consumer_id = f"unassigned-{uuid.uuid4().hex}"
    db_session.add(Consumer(id=consumer_id, consumer_group_id=group.id))
    await db_session.flush()

    result = await resolve_policy(db_session, consumer_id, "/orders", "GET")

    assert result["limit"] == settings.default_requests
    assert result["period"] == 60
    assert result["strategy"] == "moving-window"
