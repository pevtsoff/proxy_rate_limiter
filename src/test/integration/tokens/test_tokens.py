import httpx
import pytest

from proxy.api.app_container import AppContainer
from support.api_helpers import assert_error

pytestmark = pytest.mark.usefixtures("seeded")


async def test_issue_token_for_a_registered_consumer(
    client: httpx.AsyncClient, container: AppContainer
) -> None:
    response = await client.post(
        "/admin/tokens", json={"consumer_id": "free-user-1", "ttl_seconds": 120}
    )
    assert response.status_code == 201
    body = response.json()
    assert (body["token_type"], body["expires_in"]) == ("bearer", 120)
    assert container.token_codec.decode(body["access_token"]).value == "free-user-1"


async def test_no_token_for_unknown_consumers(client: httpx.AsyncClient) -> None:
    assert_error(await client.post("/admin/tokens", json={"consumer_id": "ghost"}), 404)


async def test_health(client: httpx.AsyncClient) -> None:
    assert (await client.get("/health")).json() == {"status": "ok"}
