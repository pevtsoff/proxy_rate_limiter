import dataclasses

import httpx
import pytest

from proxy.api.app_container import AppContainer
from proxy.app_factory import create_app
from proxy.config.settings import Settings
from proxy.domain.errors.rate_limit_exceeded_error import RATE_LIMIT_EXCEEDED_MESSAGE
from proxy.seeding import seed_plan
from support.api_helpers import assert_error, find_by_name

pytestmark = pytest.mark.usefixtures("seeded")


async def _hit(client: httpx.AsyncClient, headers: dict, path: str, count: int) -> list[int]:
    return [(await client.get(path, headers=headers)).status_code for _ in range(count)]


async def _set_policy_limit(client: httpx.AsyncClient, name: str, requests: int) -> None:
    policy = await find_by_name(client, "/admin/policies", "name", name)
    await client.patch(f"/admin/policies/{policy['id']}", json={"number_of_requests": requests})


async def test_cuc1_request_without_token_is_rejected(client: httpx.AsyncClient) -> None:
    assert_error(await client.get("/orders/1"), 401, "orders/1")


async def test_cuc1_request_with_invalid_token_is_rejected(client: httpx.AsyncClient) -> None:
    response = await client.get("/orders/1", headers={"Authorization": "Bearer nope"})
    assert_error(response, 401, "orders/1")


async def test_cuc1_unregistered_consumer_is_forbidden(
    client: httpx.AsyncClient, auth_headers
) -> None:
    assert_error(await client.get("/orders/1", headers=auth_headers("ghost")), 403, "orders/1")


async def test_cuc1_allowed_request_is_passed_through(
    client: httpx.AsyncClient, auth_headers
) -> None:
    response = await client.get("/orders/1", headers=auth_headers("regular-user-1"))
    assert response.status_code == 200
    assert response.json() == {"id": 1, "resource": "orders", "data": {}, "revision": None}
    assert response.headers["X-RateLimit-Limit"] == "100"
    assert response.headers["X-RateLimit-Remaining"] == "99"


async def test_cuc1_body_and_query_reach_the_target_api(
    client: httpx.AsyncClient, auth_headers
) -> None:
    response = await client.post(
        "/orders?source=test", json={"name": "pen"}, headers=auth_headers("premium-user-1")
    )
    assert response.status_code == 201
    assert response.json()["data"]["name"] == "pen"


async def test_cuc1_exhausted_quota_returns_429_with_meaningful_json(
    client: httpx.AsyncClient, auth_headers
) -> None:
    headers = auth_headers("free-user-1")
    assert await _hit(client, headers, "/orders/1", 10) == [200] * 10
    response = await client.get("/orders/1", headers=headers)
    assert response.status_code == 429
    assert response.json() == {
        "error": RATE_LIMIT_EXCEEDED_MESSAGE,
        "resource": "orders/1",
        "https_status_code": 429,
    }
    assert int(response.headers["Retry-After"]) >= 0


async def test_cuc1_quota_is_tracked_per_consumer(client: httpx.AsyncClient, auth_headers) -> None:
    await _hit(client, auth_headers("free-user-1"), "/orders/1", 11)
    assert (await client.get("/orders/1", headers=auth_headers("free-user-2"))).status_code == 200


async def test_cuc1_all_resources_policy_shares_one_quota(
    client: httpx.AsyncClient, auth_headers
) -> None:
    headers = auth_headers("free-user-1")
    await _hit(client, headers, "/orders/1", 4)
    await _hit(client, headers, "/users/2", 3)
    await _hit(client, headers, "/carts", 3)
    assert (await client.get("/carts/9", headers=headers)).status_code == 429


async def test_cuc1_resource_policy_overrides_all_resources_policy(
    client: httpx.AsyncClient, auth_headers
) -> None:
    headers = auth_headers("premium-user-1")
    assert await _hit(client, headers, "/orders/1", 4) == [200, 200, 200, 429]
    assert (await client.get("/users/1", headers=headers)).status_code == 200


async def test_cuc1_resource_group_policy_shares_quota_inside_the_group(
    client: httpx.AsyncClient, auth_headers
) -> None:
    await _set_policy_limit(client, seed_plan.REGULAR_POLICY, 3)
    headers = auth_headers("regular-user-1")
    assert await _hit(client, headers, "/orders", 2) == [200, 200]
    assert (await client.put("/orders/1", json={}, headers=headers)).status_code == 200
    assert (await client.post("/orders", json={}, headers=headers)).status_code == 429
    assert (await client.get("/users/1", headers=headers)).status_code == 200


async def test_muc3_default_policy_applies_when_nothing_is_assigned(
    client: httpx.AsyncClient, auth_headers, settings: Settings
) -> None:
    headers = auth_headers("regular-user-1")
    response = await client.get("/carts/1", headers=headers)
    assert response.headers["X-RateLimit-Limit"] == str(settings.default_policy_requests)
    await _set_policy_limit(client, settings.default_policy_name, 1)
    assert await _hit(client, headers, "/carts/1", 2) == [200, 429]


async def test_cuc1_unknown_path_is_limited_by_the_default_policy(
    client: httpx.AsyncClient, auth_headers, settings: Settings
) -> None:
    response = await client.get("/does-not-exist", headers=auth_headers("regular-user-1"))
    assert response.status_code == 404
    assert response.headers["X-RateLimit-Limit"] == str(settings.default_policy_requests)


async def test_cuc1_unreachable_target_api_is_a_json_502(
    container: AppContainer, auth_headers
) -> None:
    def refuse(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused", request=request)

    broken = httpx.AsyncClient(transport=httpx.MockTransport(refuse), base_url="http://t")
    app = create_app(dataclasses.replace(container, http_client=broken))
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://proxy"
    ) as client:
        response = await client.get("/orders/1", headers=auth_headers("regular-user-1"))
    assert_error(response, 502, "orders/1")


async def test_cuc1_limit_holds_across_several_proxy_instances(
    container: AppContainer, auth_headers
) -> None:
    """Two app instances share Redis counters, so the quota is global."""
    instances = [create_app(container), create_app(container)]
    clients = [
        httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://proxy")
        for app in instances
    ]
    headers = auth_headers("free-user-1")
    statuses = [
        (await clients[i % 2].get("/orders/1", headers=headers)).status_code for i in range(11)
    ]
    for client in clients:
        await client.aclose()
    assert statuses == [200] * 10 + [429]
