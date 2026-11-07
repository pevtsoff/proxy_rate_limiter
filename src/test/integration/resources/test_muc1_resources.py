import dataclasses

import httpx

from proxy.api.app_container import AppContainer
from proxy.app_factory import create_app
from support.api_helpers import (
    assert_error,
    assign,
    create_consumer_group,
    create_policy,
    create_resource_group,
    resource_id,
)

RESOURCES = "/admin/resources"


async def test_muc1_sync_imports_every_endpoint_from_swagger(client: httpx.AsyncClient) -> None:
    response = await client.post(f"{RESOURCES}/sync")
    assert response.json() == {"discovered": 15, "created": 15}
    resources = (await client.get(RESOURCES)).json()
    keys = {(r["method"], r["resource_path"]) for r in resources}
    for name in ("orders", "users", "carts"):
        for method, path in [
            ("GET", f"/{name}"),
            ("POST", f"/{name}"),
            ("GET", f"/{name}/{{item_id}}"),
            ("PUT", f"/{name}/{{item_id}}"),
            ("PATCH", f"/{name}/{{item_id}}"),
        ]:
            assert (method, path) in keys


async def test_muc1_reads_are_read_and_writes_are_write_by_default(
    synced: list[dict],
) -> None:
    for resource in synced:
        expected = "read" if resource["method"] == "GET" else "write"
        assert resource["io_type"] == expected


async def test_muc1_sync_is_idempotent_and_keeps_manual_changes(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    put_order = resource_id(synced, "PUT", "/orders/{item_id}")
    await client.patch(f"{RESOURCES}/{put_order}", json={"io_type": "read"})
    assert (await client.post(f"{RESOURCES}/sync")).json() == {"discovered": 15, "created": 0}
    assert (await client.get(f"{RESOURCES}/{put_order}")).json()["io_type"] == "read"


async def test_muc1_sync_reports_an_unreachable_target_api(container: AppContainer) -> None:
    broken = httpx.AsyncClient(
        transport=httpx.MockTransport(lambda request: httpx.Response(503)),
        base_url="http://target-api",
    )
    app = create_app(dataclasses.replace(container, http_client=broken))
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://proxy") as client:
        assert_error(await client.post(f"{RESOURCES}/sync"), 502)


async def test_muc1_put_resource_into_a_group_and_take_it_out(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    get_cart = resource_id(synced, "GET", "/carts")
    group_id = await create_resource_group(client, "cart-reads", [])
    moved = await client.patch(f"{RESOURCES}/{get_cart}", json={"resource_group_id": group_id})
    assert moved.json()["resource_group_id"] == group_id
    removed = await client.patch(f"{RESOURCES}/{get_cart}", json={"resource_group_id": None})
    assert removed.json()["resource_group_id"] is None


async def test_muc1_resource_group_must_exist(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    get_cart = resource_id(synced, "GET", "/carts")
    response = await client.patch(f"{RESOURCES}/{get_cart}", json={"resource_group_id": 999999})
    assert_error(response, 404)


async def test_muc1_moving_a_resource_must_not_create_an_overlap(
    client: httpx.AsyncClient, synced: list[dict]
) -> None:
    get_cart = resource_id(synced, "GET", "/carts")
    group_id = await create_resource_group(client, "carts", [])
    consumer_group = await create_consumer_group(client, "free")
    policy_id = await create_policy(client, "p")
    for target in ({"resource_id": get_cart}, {"resource_group_id": group_id}):
        relationship = "resource" if "resource_id" in target else "resource_group"
        response = await assign(
            client,
            consumer_group_id=consumer_group,
            relationship_type=relationship,
            policy_id=policy_id,
            **target,
        )
        assert response.status_code == 201
    response = await client.patch(f"{RESOURCES}/{get_cart}", json={"resource_group_id": group_id})
    assert_error(response, 409)


async def test_muc1_delete_resource(client: httpx.AsyncClient, synced: list[dict]) -> None:
    get_cart = resource_id(synced, "GET", "/carts")
    assert (await client.delete(f"{RESOURCES}/{get_cart}")).status_code == 204
    assert_error(await client.get(f"{RESOURCES}/{get_cart}"), 404)
