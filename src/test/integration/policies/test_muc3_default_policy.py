import httpx

from proxy.api.app_container import AppContainer
from proxy.config.settings import Settings
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.seeding.default_policy_bootstrapper import DefaultPolicyBootstrapper
from support.api_helpers import assert_error


async def _default_policy(client: httpx.AsyncClient) -> dict:
    policies = (await client.get("/admin/policies")).json()
    defaults = [p for p in policies if p["is_default"]]
    assert len(defaults) == 1
    return defaults[0]


async def test_muc3_default_policy_is_precreated_from_settings(
    client: httpx.AsyncClient, settings: Settings
) -> None:
    default = await _default_policy(client)
    assert default["name"] == settings.default_policy_name
    assert default["number_of_requests"] == settings.default_policy_requests
    assert default["time_period"] == settings.default_policy_period.value
    assert default["rate_limiting_strategy"] == "moving-window"


async def test_muc3_default_policy_cannot_be_deleted(client: httpx.AsyncClient) -> None:
    default = await _default_policy(client)
    assert_error(await client.delete(f"/admin/policies/{default['id']}"), 409)


async def test_muc3_default_policy_can_be_tuned(client: httpx.AsyncClient) -> None:
    default = await _default_policy(client)
    response = await client.patch(
        f"/admin/policies/{default['id']}", json={"number_of_requests": 120}
    )
    assert response.json()["number_of_requests"] == 120


async def test_muc3_bootstrap_is_idempotent(
    client: httpx.AsyncClient, container: AppContainer
) -> None:
    before = await _default_policy(client)
    async with container.session_factory() as session:
        uow = SqlAlchemyUnitOfWork(session)
        policy = await DefaultPolicyBootstrapper(uow, container.settings).ensure()
    assert policy.id == before["id"]
