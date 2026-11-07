import httpx

from support.api_helpers import assert_error, assign, create_consumer_group
from support.assignment_world import AssignmentWorld


def _resource(world: AssignmentWorld, resource_id: int) -> dict:
    return {
        "consumer_group_id": world.consumer_group_id,
        "relationship_type": "resource",
        "policy_id": world.policy_id,
        "resource_id": resource_id,
    }


def _group(world: AssignmentWorld) -> dict:
    return {
        "consumer_group_id": world.consumer_group_id,
        "relationship_type": "resource_group",
        "policy_id": world.policy_id,
        "resource_group_id": world.orders_group_id,
    }


def _all(world: AssignmentWorld) -> dict:
    return {
        "consumer_group_id": world.consumer_group_id,
        "relationship_type": "all_resources",
        "policy_id": world.policy_id,
    }


async def test_muc1_assign_policy_to_one_resource(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    response = await assign(client, **_resource(world, world.get_cart_id))
    assert response.status_code == 201
    assert response.json()["resource_id"] == world.get_cart_id
    assert response.json()["resource_group_id"] is None


async def test_muc1_assign_policy_to_a_resource_group(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    response = await assign(client, **_group(world))
    assert response.status_code == 201
    assert response.json()["relationship_type"] == "resource_group"


async def test_muc1_assign_one_policy_to_all_resources(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    response = await assign(client, **_all(world))
    assert response.status_code == 201


async def test_muc1_resource_of_an_assigned_group_cannot_get_its_own_policy(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    await assign(client, **_group(world))
    assert_error(await assign(client, **_resource(world, world.get_order_id)), 409)


async def test_muc1_group_cannot_be_assigned_when_a_member_has_its_own_policy(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    await assign(client, **_resource(world, world.get_order_id))
    assert_error(await assign(client, **_group(world)), 409)


async def test_muc1_duplicate_assignments_are_rejected(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    for payload in (_resource(world, world.get_cart_id), _group(world), _all(world)):
        assert (await assign(client, **payload)).status_code == 201
        assert_error(await assign(client, **payload), 409)


async def test_muc1_all_resources_coexists_with_specific_assignments(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    assert (await assign(client, **_group(world))).status_code == 201
    assert (await assign(client, **_resource(world, world.get_cart_id))).status_code == 201
    assert (await assign(client, **_all(world))).status_code == 201


async def test_muc1_other_consumer_groups_are_independent(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    await assign(client, **_group(world))
    other_group = await create_consumer_group(client, "premium")
    payload = {**_resource(world, world.get_order_id), "consumer_group_id": other_group}
    assert (await assign(client, **payload)).status_code == 201


async def test_muc1_target_must_match_relationship_type(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    payload = {**_resource(world, world.get_cart_id), "resource_group_id": world.orders_group_id}
    assert_error(await assign(client, **payload), 422)
    assert_error(await assign(client, **{**_all(world), "resource_id": world.get_cart_id}), 422)


async def test_muc1_referenced_entities_must_exist(
    client: httpx.AsyncClient, world: AssignmentWorld
) -> None:
    assert_error(await assign(client, **{**_all(world), "policy_id": 999999}), 404)
    assert_error(await assign(client, **{**_all(world), "consumer_group_id": 999999}), 404)
    assert_error(await assign(client, **_resource(world, 999999)), 404)
