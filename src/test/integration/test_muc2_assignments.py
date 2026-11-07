"""Manager assignment CRUD and consistency integration coverage."""

import uuid

import pytest
from app.models import ConsumerGroup, Policy, Resource, ResourceGroup
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession


@pytest.mark.asyncio
async def test_muc2_assignment_crud_and_conflicts(
    api_client: AsyncClient, db_session: AsyncSession
) -> None:
    """Assignments support CRUD and reject mixed resource scopes."""
    payload = await assignment_payload(db_session)
    response = await exercise_assignment_crud(api_client, payload)
    assert response == (201, 409, 200, 204, 404)


async def assignment_payload(session: AsyncSession) -> dict[str, int | str]:
    group = ConsumerGroup(group_name=await available_group_name(session))
    policy = Policy(
        name=f"assignment-{uuid.uuid4().hex}", number_of_requests=10, time_period="minute"
    )
    resource_group = ResourceGroup(name=f"assignment-{uuid.uuid4().hex}")
    session.add_all([group, policy, resource_group])
    await session.flush()
    resource = Resource(
        io_type="read",
        method="GET",
        resource_path=f"/assignment-{uuid.uuid4().hex}",
        resource_group_id=resource_group.id,
    )
    session.add(resource)
    await session.flush()
    return {
        "consumer_group_id": group.id,
        "relationship_type": "resource_group",
        "policy_id": policy.id,
        "resource_id": resource_group.id,
        "child_resource_id": resource.id,
    }


async def exercise_assignment_crud(
    client: AsyncClient, payload: dict[str, int | str]
) -> tuple[int, int, int, int, int]:
    create_payload = {key: value for key, value in payload.items() if key != "child_resource_id"}
    created = await client.post("/admin-api/assignments", json=create_payload)
    assignment_id = created.json()["id"]
    conflicting = {
        **create_payload,
        "relationship_type": "resource",
        "resource_id": payload["child_resource_id"],
    }
    conflict = await client.post("/admin-api/assignments", json=conflicting)
    updated = await client.put(f"/admin-api/assignments/{assignment_id}", json=create_payload)
    deleted = await client.delete(f"/admin-api/assignments/{assignment_id}")
    missing = await client.delete(f"/admin-api/assignments/{assignment_id}")
    return (
        created.status_code,
        conflict.status_code,
        updated.status_code,
        deleted.status_code,
        missing.status_code,
    )


async def available_group_name(session: AsyncSession) -> str:
    """Choose a valid unused group name for the rollback-isolated test."""
    existing = set((await session.scalars(select(ConsumerGroup.group_name))).all())
    return next(name for name in ("premium", "regular", "free", "internal") if name not in existing)
