from dataclasses import dataclass


@dataclass(frozen=True)
class AssignmentWorld:
    """free group, a policy, `orders` resource group and two of its resources."""

    consumer_group_id: int
    policy_id: int
    orders_group_id: int
    get_order_id: int
    get_orders_id: int
    get_cart_id: int
