import httpx
import pytest

from support.api_helpers import (
    create_consumer_group,
    create_policy,
    create_resource_group,
    resource_id,
)
from support.assignment_world import AssignmentWorld


@pytest.fixture
async def world(client: httpx.AsyncClient, synced: list[dict]) -> AssignmentWorld:
    get_order = resource_id(synced, "GET", "/orders/{item_id}")
    get_orders = resource_id(synced, "GET", "/orders")
    return AssignmentWorld(
        consumer_group_id=await create_consumer_group(client, "free"),
        policy_id=await create_policy(client, "assignable"),
        orders_group_id=await create_resource_group(client, "orders", [get_order, get_orders]),
        get_order_id=get_order,
        get_orders_id=get_orders,
        get_cart_id=resource_id(synced, "GET", "/carts"),
    )
