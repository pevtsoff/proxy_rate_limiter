import httpx
import pytest

from support.api_helpers import assert_error, assign, create_consumer_group, create_policy
from support.assignment_world import AssignmentWorld

ASSIGNMENTS = "/admin/consumer-groups-policies"


@pytest.fixture
async def resource_assignment(client: httpx.AsyncClient, world: AssignmentWorld) -> dict:
    response = await assign(
        client,
        consumer_group_id=world.consumer_group_id,
        relationship_type="resource",
        policy_id=world.policy_id,
        resource_id=world.get_order_id,
    )
    return response.json()


async def test_muc2_list_assignments_of_one_consumer_group(
    client: httpx.AsyncClient, world: AssignmentWorld, resource_assignment: dict
) -> None:
    other_group = await create_consumer_group(client, "premium")
    await assign(
        client,
        consumer_group_id=other_group,
        relationship_type="all_resources",
        policy_id=world.policy_id,
    )
    response = await client.get(ASSIGNMENTS, params={"consumer_group_id": world.consumer_group_id})
    assert [a["id"] for a in response.json()] == [resource_assignment["id"]]


async def test_muc2_get_assignment(client: httpx.AsyncClient, resource_assignment: dict) -> None:
    response = await client.get(f"{ASSIGNMENTS}/{resource_assignment['id']}")
    assert response.json() == resource_assignment


async def test_muc2_change_the_policy_of_an_assignment(
    client: httpx.AsyncClient, resource_assignment: dict
) -> None:
    new_policy = await create_policy(client, "stricter", 1)
    response = await client.patch(
        f"{ASSIGNMENTS}/{resource_assignment['id']}", json={"policy_id": new_policy}
    )
    assert response.json()["policy_id"] == new_policy
    assert response.json()["resource_id"] == resource_assignment["resource_id"]


async def test_muc2_retarget_assignment_to_another_resource(
    client: httpx.AsyncClient, world: AssignmentWorld, resource_assignment: dict
) -> None:
    response = await client.patch(
        f"{ASSIGNMENTS}/{resource_assignment['id']}",
        json={"relationship_type": "resource", "resource_id": world.get_cart_id},
    )
    assert response.json()["resource_id"] == world.get_cart_id


async def test_muc2_retarget_from_resource_to_its_own_group_is_allowed(
    client: httpx.AsyncClient, world: AssignmentWorld, resource_assignment: dict
) -> None:
    """The assignment being changed does not conflict with itself."""
    response = await client.patch(
        f"{ASSIGNMENTS}/{resource_assignment['id']}",
        json={"relationship_type": "resource_group", "resource_group_id": world.orders_group_id},
    )
    assert response.status_code == 200
    assert response.json()["resource_id"] is None


async def test_muc2_retarget_into_a_conflict_is_rejected(
    client: httpx.AsyncClient, world: AssignmentWorld, resource_assignment: dict
) -> None:
    other = await assign(
        client,
        consumer_group_id=world.consumer_group_id,
        relationship_type="resource",
        policy_id=world.policy_id,
        resource_id=world.get_cart_id,
    )
    response = await client.patch(
        f"{ASSIGNMENTS}/{other.json()['id']}",
        json={"relationship_type": "resource_group", "resource_group_id": world.orders_group_id},
    )
    assert_error(response, 409)


async def test_muc2_changing_target_needs_relationship_type(
    client: httpx.AsyncClient, world: AssignmentWorld, resource_assignment: dict
) -> None:
    response = await client.patch(
        f"{ASSIGNMENTS}/{resource_assignment['id']}", json={"resource_id": world.get_cart_id}
    )
    assert_error(response, 422)


async def test_muc2_delete_assignment(client: httpx.AsyncClient, resource_assignment: dict) -> None:
    url = f"{ASSIGNMENTS}/{resource_assignment['id']}"
    assert (await client.delete(url)).status_code == 204
    assert_error(await client.get(url), 404)
