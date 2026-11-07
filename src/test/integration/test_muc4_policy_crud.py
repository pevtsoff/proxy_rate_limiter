"""Manager policy CRUD and validation integration coverage."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_muc4_policy_crud_and_validation(api_client: AsyncClient) -> None:
    """Managers can create, list, replace, and delete policies."""
    payload = {
        "name": "crud-policy-test",
        "rate_limiting_strategy": "moving-window",
        "number_of_requests": 3,
        "time_period": "minute",
    }
    created = await api_client.post("/admin-api/policies", json=payload)
    assert created.status_code == 201
    policy_id = created.json()["id"]

    listing = await api_client.get("/admin-api/policies")
    assert any(policy["id"] == policy_id for policy in listing.json())

    updated_payload = {**payload, "number_of_requests": 8}
    updated = await api_client.put(f"/admin-api/policies/{policy_id}", json=updated_payload)
    assert updated.status_code == 200
    assert updated.json()["number_of_requests"] == 8

    invalid = await api_client.post(
        "/admin-api/policies", json={**payload, "time_period": "fortnight"}
    )
    assert invalid.status_code == 422

    deleted = await api_client.delete(f"/admin-api/policies/{policy_id}")
    assert deleted.status_code == 204
    missing = await api_client.delete(f"/admin-api/policies/{policy_id}")
    assert missing.status_code == 404
