import httpx
import pytest

from support.api_helpers import assert_error, create_consumer_group

CONSUMERS = "/admin/consumers"


@pytest.fixture
async def free_group(client: httpx.AsyncClient) -> int:
    return await create_consumer_group(client, "free", "first")


async def test_muc1_create_consumer_in_a_group(client: httpx.AsyncClient, free_group: int) -> None:
    response = await client.post(CONSUMERS, json={"id": "second", "consumer_group_id": free_group})
    assert response.status_code == 201
    assert response.json() == {"id": "second", "consumer_group_id": free_group}


async def test_muc1_consumer_cannot_exist_without_a_group(client: httpx.AsyncClient) -> None:
    assert_error(await client.post(CONSUMERS, json={"id": "orphan"}), 422)


async def test_muc1_consumer_group_must_exist(client: httpx.AsyncClient) -> None:
    response = await client.post(CONSUMERS, json={"id": "x", "consumer_group_id": 999999})
    assert_error(response, 404)


async def test_muc1_duplicate_consumer_is_rejected(
    client: httpx.AsyncClient, free_group: int
) -> None:
    response = await client.post(CONSUMERS, json={"id": "first", "consumer_group_id": free_group})
    assert_error(response, 409)


async def test_muc1_move_consumer_to_another_group(
    client: httpx.AsyncClient, free_group: int
) -> None:
    await client.post(CONSUMERS, json={"id": "second", "consumer_group_id": free_group})
    premium = await create_consumer_group(client, "premium", "vip")
    response = await client.patch(f"{CONSUMERS}/second", json={"consumer_group_id": premium})
    assert response.json()["consumer_group_id"] == premium


async def test_muc1_last_consumer_cannot_leave_its_group(
    client: httpx.AsyncClient, free_group: int
) -> None:
    premium = await create_consumer_group(client, "premium", "vip")
    response = await client.patch(f"{CONSUMERS}/first", json={"consumer_group_id": premium})
    assert_error(response, 409)


async def test_muc1_last_consumer_cannot_be_deleted(
    client: httpx.AsyncClient, free_group: int
) -> None:
    assert_error(await client.delete(f"{CONSUMERS}/first"), 409)


async def test_muc1_delete_consumer(client: httpx.AsyncClient, free_group: int) -> None:
    await client.post(CONSUMERS, json={"id": "second", "consumer_group_id": free_group})
    assert (await client.delete(f"{CONSUMERS}/second")).status_code == 204
    assert_error(await client.get(f"{CONSUMERS}/second"), 404)


async def test_muc1_list_consumers_of_one_group(client: httpx.AsyncClient, free_group: int) -> None:
    await create_consumer_group(client, "premium", "vip")
    response = await client.get(CONSUMERS, params={"consumer_group_id": free_group})
    assert [c["id"] for c in response.json()] == ["first"]
