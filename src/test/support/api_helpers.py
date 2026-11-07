"""Small helpers that keep integration tests focused on behaviour."""

import httpx


def assert_error(response: httpx.Response, status: int, resource: str | None = None) -> dict:
    assert response.status_code == status, response.text
    body = response.json()
    assert set(body) == {"error", "resource", "https_status_code"}
    assert body["https_status_code"] == status
    if resource is not None:
        assert body["resource"] == resource
    return body


def resource_id(resources: list[dict], method: str, path: str) -> int:
    return next(r["id"] for r in resources if (r["method"], r["resource_path"]) == (method, path))


async def create_consumer_group(
    client: httpx.AsyncClient, group_name: str, *consumer_ids: str
) -> int:
    ids = list(consumer_ids) or [f"{group_name}-consumer"]
    response = await client.post(
        "/admin/consumer-groups", json={"group_name": group_name, "consumer_ids": ids}
    )
    assert response.status_code == 201, response.text
    return response.json()["id"]


async def create_policy(
    client: httpx.AsyncClient, name: str, requests: int = 5, period: str = "minute"
) -> int:
    response = await client.post(
        "/admin/policies",
        json={"name": name, "number_of_requests": requests, "time_period": period},
    )
    assert response.status_code == 201, response.text
    return response.json()["id"]


async def create_resource_group(
    client: httpx.AsyncClient, name: str, resource_ids: list[int]
) -> int:
    response = await client.post(
        "/admin/resource-groups", json={"name": name, "resource_ids": resource_ids}
    )
    assert response.status_code == 201, response.text
    return response.json()["id"]


async def assign(client: httpx.AsyncClient, **payload: object) -> httpx.Response:
    return await client.post("/admin/consumer-groups-policies", json=payload)


async def find_by_name(client: httpx.AsyncClient, path: str, key: str, value: str) -> dict:
    items = (await client.get(path)).json()
    return next(item for item in items if item[key] == value)
