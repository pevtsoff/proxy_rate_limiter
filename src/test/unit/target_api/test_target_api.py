import logging

import httpx
import pytest

from target_api.app_factory import create_app


@pytest.fixture
async def client() -> httpx.AsyncClient:
    transport = httpx.ASGITransport(app=create_app())
    return httpx.AsyncClient(transport=transport, base_url="http://target")


@pytest.mark.parametrize("resource", ["orders", "users", "carts"])
async def test_get_is_idempotent(client: httpx.AsyncClient, resource: str) -> None:
    first = await client.get(f"/{resource}/7")
    second = await client.get(f"/{resource}/7")
    assert first.status_code == 200
    assert (
        first.json()
        == second.json()
        == {
            "id": 7,
            "resource": resource,
            "data": {},
            "revision": None,
        }
    )


@pytest.mark.parametrize("resource", ["orders", "users", "carts"])
async def test_put_is_idempotent(client: httpx.AsyncClient, resource: str) -> None:
    first = await client.put(f"/{resource}/1", json={"name": "x"})
    second = await client.put(f"/{resource}/1", json={"name": "x"})
    assert first.json() == second.json()


@pytest.mark.parametrize("resource", ["orders", "users", "carts"])
async def test_post_is_not_idempotent(client: httpx.AsyncClient, resource: str) -> None:
    first = await client.post(f"/{resource}", json={"name": "x"})
    second = await client.post(f"/{resource}", json={"name": "x"})
    assert first.status_code == 201
    assert first.json()["id"] != second.json()["id"]


@pytest.mark.parametrize("resource", ["orders", "users", "carts"])
async def test_patch_is_not_idempotent(client: httpx.AsyncClient, resource: str) -> None:
    first = await client.patch(f"/{resource}/1", json={"name": "x"})
    second = await client.patch(f"/{resource}/1", json={"name": "x"})
    assert first.json()["revision"] != second.json()["revision"]


async def test_list_returns_static_items(client: httpx.AsyncClient) -> None:
    response = await client.get("/orders")
    assert [item["id"] for item in response.json()] == [1, 2, 3]


async def test_every_request_is_logged(
    client: httpx.AsyncClient, caplog: pytest.LogCaptureFixture
) -> None:
    with caplog.at_level(logging.INFO, logger="target_api.requests"):
        await client.get("/users/1?verbose=true", headers={"x-consumer-id": "alice"})
    message = next(r.getMessage() for r in caplog.records if r.name == "target_api.requests")
    assert "GET /users/1" in message
    assert "consumer=alice" in message
    assert "status=200" in message
