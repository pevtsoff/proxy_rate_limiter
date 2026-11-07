import httpx

from support.api_helpers import (
    assert_error,
    assign,
    create_consumer_group,
    create_policy,
    create_resource_group,
    resource_id,
)

GROUPS = "/admin/resource-groups"


async def test_muc1_create_resource_group_with_members(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    members = [resource_id(synced, "GET", "/orders"), resource_id(synced, "POST", "/orders")]
    response = await client.post(
        GROUPS, json={"name": "orders", "description": "all orders", "resource_ids": members}
    )
    assert response.status_code == 201
    assert response.json()["resource_ids"] == sorted(members)
    resource = (await client.get(f"/admin/resources/{members[0]}")).json()
    assert resource["resource_group_id"] == response.json()["id"]


async def test_muc1_resource_group_names_are_unique(client: httpx.AsyncClient) -> None:
    await create_resource_group(client, "dup", [])
    assert_error(await client.post(GROUPS, json={"name": "dup"}), 409)


async def test_muc1_resource_group_members_must_exist(client: httpx.AsyncClient) -> None:
    assert_error(await client.post(GROUPS, json={"name": "g", "resource_ids": [999999]}), 404)


async def test_muc1_replace_resource_group_membership(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    first, second = resource_id(synced, "GET", "/users"), resource_id(synced, "POST", "/users")
    group_id = await create_resource_group(client, "users", [first])
    response = await client.patch(
        f"{GROUPS}/{group_id}", json={"name": "people", "resource_ids": [second]}
    )
    assert response.json()["name"] == "people"
    assert response.json()["resource_ids"] == [second]
    assert (await client.get(f"/admin/resources/{first}")).json()["resource_group_id"] is None


async def test_muc1_adding_a_member_must_not_create_an_overlap(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    get_user = resource_id(synced, "GET", "/users")
    group_id = await create_resource_group(client, "users", [])
    consumer_group = await create_consumer_group(client, "free")
    policy_id = await create_policy(client, "p")
    await assign(
        client,
        consumer_group_id=consumer_group,
        relationship_type="resource",
        policy_id=policy_id,
        resource_id=get_user,
    )
    await assign(
        client,
        consumer_group_id=consumer_group,
        relationship_type="resource_group",
        policy_id=policy_id,
        resource_group_id=group_id,
    )
    response = await client.patch(f"{GROUPS}/{group_id}", json={"resource_ids": [get_user]})
    assert_error(response, 409)


async def test_muc1_deleting_a_group_ungroups_members_and_drops_its_assignments(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    member = resource_id(synced, "GET", "/carts")
    group_id = await create_resource_group(client, "carts", [member])
    consumer_group = await create_consumer_group(client, "free")
    created = await assign(
        client,
        consumer_group_id=consumer_group,
        relationship_type="resource_group",
        policy_id=await create_policy(client, "p"),
        resource_group_id=group_id,
    )
    assert (await client.delete(f"{GROUPS}/{group_id}")).status_code == 204
    assert (await client.get(f"/admin/resources/{member}")).json()["resource_group_id"] is None
    assignment_id = created.json()["id"]
    assert_error(await client.get(f"/admin/consumer-groups-policies/{assignment_id}"), 404)
