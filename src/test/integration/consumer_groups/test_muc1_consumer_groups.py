import httpx

from support.api_helpers import assert_error, create_consumer_group

GROUPS = "/admin/consumer-groups"


async def test_muc1_create_consumer_group_with_its_consumers(client: httpx.AsyncClient) -> None:
    response = await client.post(
        GROUPS, json={"group_name": "premium", "consumer_ids": ["alice", "bob"]}
    )
    assert response.status_code == 201
    body = response.json()
    assert body["group_name"] == "premium"
    assert body["consumer_ids"] == ["alice", "bob"]
    consumers = await client.get("/admin/consumers", params={"consumer_group_id": body["id"]})
    assert {c["id"] for c in consumers.json()} == {"alice", "bob"}


async def test_muc1_consumer_group_needs_at_least_one_consumer(
    client: httpx.AsyncClient,
) -> None:
    response = await client.post(GROUPS, json={"group_name": "free", "consumer_ids": []})
    assert_error(response, 422, "admin/consumer-groups")


async def test_muc1_consumer_group_accepts_a_custom_name(client: httpx.AsyncClient) -> None:
    response = await client.post(GROUPS, json={"group_name": "gold", "consumer_ids": ["a"]})
    assert response.status_code == 201
    assert response.json()["group_name"] == "gold"


async def test_muc1_duplicate_consumer_group_is_rejected(client: httpx.AsyncClient) -> None:
    await create_consumer_group(client, "premium", "alice")
    response = await client.post(GROUPS, json={"group_name": "premium", "consumer_ids": ["b"]})
    assert_error(response, 409)


async def test_muc1_existing_consumer_cannot_be_added_to_a_new_group(
    client: httpx.AsyncClient,
) -> None:
    await create_consumer_group(client, "free", "alice")
    response = await client.post(GROUPS, json={"group_name": "regular", "consumer_ids": ["alice"]})
    assert_error(response, 409)


async def test_muc1_get_and_list_consumer_groups(client: httpx.AsyncClient) -> None:
    group_id = await create_consumer_group(client, "internal", "svc")
    assert (await client.get(f"{GROUPS}/{group_id}")).json()["consumer_ids"] == ["svc"]
    assert group_id in [g["id"] for g in (await client.get(GROUPS)).json()]


async def test_muc1_rename_consumer_group(client: httpx.AsyncClient) -> None:
    group_id = await create_consumer_group(client, "regular", "carol")
    response = await client.patch(f"{GROUPS}/{group_id}", json={"group_name": "premium"})
    assert response.json()["group_name"] == "premium"


async def test_muc1_rename_to_an_existing_group_name_conflicts(
    client: httpx.AsyncClient,
) -> None:
    await create_consumer_group(client, "free", "a")
    group_id = await create_consumer_group(client, "regular", "b")
    response = await client.patch(f"{GROUPS}/{group_id}", json={"group_name": "free"})
    assert_error(response, 409)


async def test_muc1_deleting_a_group_deletes_its_consumers(client: httpx.AsyncClient) -> None:
    group_id = await create_consumer_group(client, "free", "dave")
    assert (await client.delete(f"{GROUPS}/{group_id}")).status_code == 204
    assert_error(await client.get(f"{GROUPS}/{group_id}"), 404)
    assert_error(await client.get("/admin/consumers/dave"), 404)


async def test_muc1_unknown_consumer_group_is_a_json_404(client: httpx.AsyncClient) -> None:
    response = await client.get(f"{GROUPS}/999999")
    body = assert_error(response, 404, "admin/consumer-groups/999999")
    assert "999999" in body["error"]
