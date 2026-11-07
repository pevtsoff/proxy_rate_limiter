"""Manager assignment CRUD and consistency integration coverage."""

import uuid

import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.entities import ConsumerGroup, Policy, Resource, ResourceGroup


@pytest.mark.asyncio
async def test_muc2_assignment_crud_and_conflicts(
    api_client: AsyncClient, db_session: AsyncSession
) -> None:
    """Assignments support CRUD and reject mixed resource scopes."""
    group_name = await available_group_name(db_session)
    group = ConsumerGroup(group_name=group_name)
    policy = Policy(name=f"assignment-{uuid.uuid4().hex}", number_of_requests=10, time_period="minute")
    resource_group = ResourceGroup(name=f"assignment-{uuid.uuid4().hex}")
    db_session.add_all([group, policy, resource_group])
    await db_session.flush()
    resource = Resource(
        io_type="read", method="GET", resource_path=f"/assignment-{uuid.uuid4().hex}",
        resource_group_id=resource_group.id,
    )
    db_session.add(resource)
    await db_session.flush()
    group_id, policy_id, resource_group_id, resource_id = group.id, policy.id, resource_group.id, resource.id

    payload = {
        "consumer_group_id": group_id,
        "relationship_type": "resource_group",
        "policy_id": policy_id,
        "resource_id": resource_group_id,
    }
    created = await api_client.post("/admin-api/assignments", json=payload)
    assert created.status_code == 201
    assignment_id = created.json()["id"]
    listing = await api_client.get("/admin-api/assignments")
    assert any(item["id"] == assignment_id for item in listing.json())

    conflicting = {**payload, "relationship_type": "resource", "resource_id": resource_id}
    conflict_response = await api_client.post("/admin-api/assignments", json=conflicting)
    assert conflict_response.status_code == 409

    updated = await api_client.put(f"/admin-api/assignments/{assignment_id}", json=payload)
    assert updated.status_code == 200
    deleted = await api_client.delete(f"/admin-api/assignments/{assignment_id}")
    assert deleted.status_code == 204
    missing = await api_client.delete(f"/admin-api/assignments/{assignment_id}")
    assert missing.status_code == 404


async def available_group_name(session: AsyncSession) -> str:
    """Choose a valid unused group name for the rollback-isolated test."""
    existing = set((await session.scalars(select(ConsumerGroup.group_name))).all())
    return next(name for name in ("premium", "regular", "free", "internal") if name not in existing)
