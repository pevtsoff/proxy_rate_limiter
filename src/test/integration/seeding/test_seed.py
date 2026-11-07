import httpx

from proxy.api.app_container import AppContainer
from proxy.infrastructure.openapi_spec_source import OpenApiSpecSource
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.seeding import seed_plan
from proxy.seeding.database_seeder import DatabaseSeeder


async def test_seed_loads_the_demo_data(client: httpx.AsyncClient, seeded: None) -> None:
    groups = (await client.get("/admin/consumer-groups")).json()
    assert {g["group_name"] for g in groups} == {"free", "regular", "premium", "internal"}
    policy_names = {p["name"] for p in (await client.get("/admin/policies")).json()}
    assert {name for name, *_ in seed_plan.POLICIES} <= policy_names
    assignments = (await client.get("/admin/consumer-groups-policies")).json()
    assert len(assignments) == len(seed_plan.ASSIGNMENTS)
    assert len((await client.get("/admin/resources")).json()) == 15


async def test_seed_is_idempotent(container: AppContainer, seeded: None) -> None:
    async with container.session_factory() as session:
        uow = SqlAlchemyUnitOfWork(session)
        source = OpenApiSpecSource(container.http_client, container.settings.target_openapi_path)
        assert await DatabaseSeeder(uow, source, container.settings).seed() is False
