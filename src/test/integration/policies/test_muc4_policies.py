import httpx
import pytest

from proxy.domain.enums.rate_limiting_strategy import SUPPORTED_STRATEGIES
from support.api_helpers import assert_error, assign, create_consumer_group, create_policy

POLICIES = "/admin/policies"


async def test_muc4_create_policy_defaults_to_moving_window(client: httpx.AsyncClient) -> None:
    response = await client.post(
        POLICIES, json={"name": "basic", "number_of_requests": 10, "time_period": "minute"}
    )
    assert response.status_code == 201
    body = response.json()
    assert body["rate_limiting_strategy"] == "moving-window"
    assert body["is_default"] is False


@pytest.mark.parametrize("strategy", SUPPORTED_STRATEGIES)
async def test_muc4_every_slowapi_strategy_is_accepted(
    client: httpx.AsyncClient, strategy: str
) -> None:
    response = await client.post(
        POLICIES,
        json={
            "name": f"p-{strategy}",
            "rate_limiting_strategy": strategy,
            "number_of_requests": 1,
            "time_period": "second",
        },
    )
    assert response.json()["rate_limiting_strategy"] == strategy


@pytest.mark.parametrize("period", ["second", "minute", "hour", "day", "week"])
async def test_muc4_every_time_period_is_accepted(client: httpx.AsyncClient, period: str) -> None:
    await create_policy(client, f"p-{period}", period=period)


@pytest.mark.parametrize(
    "payload",
    [
        {"rate_limiting_strategy": "leaky-bucket"},
        {"number_of_requests": 0},
        {"number_of_requests": -5},
        {"time_period": "month"},
        {"name": ""},
    ],
)
async def test_muc4_invalid_policies_are_rejected(client: httpx.AsyncClient, payload: dict) -> None:
    body = {"name": "bad", "number_of_requests": 1, "time_period": "minute", **payload}
    assert_error(await client.post(POLICIES, json=body), 422)


async def test_muc4_policy_names_are_unique(client: httpx.AsyncClient) -> None:
    await create_policy(client, "same")
    response = await client.post(
        POLICIES, json={"name": "same", "number_of_requests": 1, "time_period": "minute"}
    )
    assert_error(response, 409)


async def test_muc4_read_and_list_policies(client: httpx.AsyncClient) -> None:
    policy_id = await create_policy(client, "readable", 7, "hour")
    body = (await client.get(f"{POLICIES}/{policy_id}")).json()
    assert (body["number_of_requests"], body["time_period"]) == (7, "hour")
    assert "readable" in [p["name"] for p in (await client.get(POLICIES)).json()]


async def test_muc4_update_policy(client: httpx.AsyncClient) -> None:
    policy_id = await create_policy(client, "changing")
    response = await client.patch(
        f"{POLICIES}/{policy_id}",
        json={"number_of_requests": 50, "rate_limiting_strategy": "fixed-window"},
    )
    body = response.json()
    assert (body["number_of_requests"], body["rate_limiting_strategy"]) == (50, "fixed-window")
    assert body["name"] == "changing"


async def test_muc4_update_to_an_existing_name_conflicts(client: httpx.AsyncClient) -> None:
    await create_policy(client, "taken")
    policy_id = await create_policy(client, "other")
    assert_error(await client.patch(f"{POLICIES}/{policy_id}", json={"name": "taken"}), 409)


async def test_muc4_delete_policy(client: httpx.AsyncClient) -> None:
    policy_id = await create_policy(client, "temporary")
    assert (await client.delete(f"{POLICIES}/{policy_id}")).status_code == 204
    assert_error(await client.get(f"{POLICIES}/{policy_id}"), 404)


async def test_muc4_assigned_policy_cannot_be_deleted(client: httpx.AsyncClient) -> None:
    policy_id = await create_policy(client, "in-use")
    group_id = await create_consumer_group(client, "free")
    await assign(
        client, consumer_group_id=group_id, relationship_type="all_resources", policy_id=policy_id
    )
    assert_error(await client.delete(f"{POLICIES}/{policy_id}"), 409)


async def test_muc4_list_supported_strategies(client: httpx.AsyncClient) -> None:
    body = (await client.get(f"{POLICIES}/strategies")).json()
    assert body == {"strategies": list(SUPPORTED_STRATEGIES), "default": "moving-window"}


async def test_muc4_unknown_policy_is_a_json_404(client: httpx.AsyncClient) -> None:
    assert_error(await client.get(f"{POLICIES}/424242"), 404, "admin/policies/424242")
