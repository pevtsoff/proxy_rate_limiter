import httpx
import pytest

from proxy.admin_ui.admin_ui_mounter import bind_view
from proxy.admin_ui.consumer_admin import ConsumerAdmin
from proxy.admin_ui.consumer_group_policy_admin import ConsumerGroupPolicyAdmin
from proxy.admin_ui.policy_admin import PolicyAdmin
from proxy.api.app_container import AppContainer
from proxy.db.models.policy import Policy
from proxy.domain.errors.conflict_error import ConflictError
from proxy.seeding import seed_plan
from support.api_helpers import find_by_name, resource_id

pytestmark = pytest.mark.usefixtures("seeded")

VIEW_IDENTITIES = [
    "consumer-group",
    "consumer",
    "policy",
    "resource-group",
    "resource",
    "consumer-group-policy",
]


@pytest.mark.parametrize("identity", VIEW_IDENTITIES)
async def test_muc6_every_model_has_an_admin_list_page(
    client: httpx.AsyncClient, identity: str
) -> None:
    response = await client.get(f"/admin-ui/{identity}/list")
    assert response.status_code == 200


async def test_muc6_admin_ui_shows_seeded_policies(client: httpx.AsyncClient) -> None:
    page = await client.get("/admin-ui/policy/list")
    assert seed_plan.FREE_POLICY in page.text


async def test_muc6_admin_ui_create_form_creates_a_policy(client: httpx.AsyncClient) -> None:
    form = {
        "name": "from-admin-ui",
        "rate_limiting_strategy": "fixed-window",
        "number_of_requests": "7",
        "time_period": "minute",
    }
    response = await client.post("/admin-ui/policy/create", data=form)
    assert response.status_code in (302, 303)
    policy = await find_by_name(client, "/admin/policies", "name", "from-admin-ui")
    assert policy["number_of_requests"] == 7


async def test_muc6_admin_ui_applies_the_api_conflict_rules(
    client: httpx.AsyncClient, container: AppContainer
) -> None:
    """Seed assigns resource group `orders` to `regular`; a resource-level one must fail."""
    resources = (await client.get("/admin/resources")).json()
    regular = await find_by_name(client, "/admin/consumer-groups", "group_name", "regular")
    view = bind_view(ConsumerGroupPolicyAdmin, container)()
    data = {
        "consumer_group": str(regular["id"]),
        "relationship_type": "resource",
        "policy": "1",
        "resource": str(resource_id(resources, "GET", "/orders")),
        "resource_group": "",
    }
    with pytest.raises(ConflictError):
        await view.on_model_change(data, ConsumerGroupPolicyAdmin.model(), True, None)


async def test_muc6_admin_ui_protects_the_default_policy(container: AppContainer) -> None:
    view = bind_view(PolicyAdmin, container)()
    with pytest.raises(ConflictError):
        await view.on_model_delete(Policy(id=1, name="default", is_default=True), None)


async def test_muc6_admin_ui_keeps_consumer_groups_non_empty(
    client: httpx.AsyncClient, container: AppContainer
) -> None:
    premium_user = (await client.get("/admin/consumers/premium-user-1")).json()
    view = bind_view(ConsumerAdmin, container)()
    consumer = ConsumerAdmin.model(
        id=premium_user["id"], consumer_group_id=premium_user["consumer_group_id"]
    )
    with pytest.raises(ConflictError):
        await view.on_model_delete(consumer, None)


async def test_muc6_admin_ui_changes_invalidate_the_policy_cache(
    client: httpx.AsyncClient, container: AppContainer, auth_headers
) -> None:
    await client.get("/orders/1", headers=auth_headers("free-user-1"))
    assert await container.policy_cache.size() == 1
    await bind_view(PolicyAdmin, container)().after_model_change({}, None, False, None)
    assert await container.policy_cache.size() == 0
