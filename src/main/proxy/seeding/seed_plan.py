"""Demo data used by `./seed.sh` and by the integration test fixtures."""

from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.enums.time_period import TimePeriod

FREE_POLICY = "free-10-per-minute"
REGULAR_POLICY = "regular-100-per-minute"
PREMIUM_POLICY = "premium-500-per-minute"
INTERNAL_POLICY = "internal-10000-per-minute"
CART_WRITES_POLICY = "cart-writes-5-per-minute"
ORDER_ITEM_BURST_POLICY = "order-item-3-per-second"

# (name, strategy, number_of_requests, time_period)
POLICIES = (
    (FREE_POLICY, "fixed-window", 10, TimePeriod.MINUTE),
    (REGULAR_POLICY, "moving-window", 100, TimePeriod.MINUTE),
    (PREMIUM_POLICY, "moving-window", 500, TimePeriod.MINUTE),
    (INTERNAL_POLICY, "sliding-window-counter", 10_000, TimePeriod.MINUTE),
    (CART_WRITES_POLICY, "moving-window", 5, TimePeriod.MINUTE),
    (ORDER_ITEM_BURST_POLICY, "moving-window", 3, TimePeriod.SECOND),
)

# resource group name -> path prefix of its member resources
RESOURCE_GROUPS = {"orders": "/orders", "users": "/users", "carts": "/carts"}

CONSUMER_GROUPS = {
    "free": ("free-user-1", "free-user-2"),
    "regular": ("regular-user-1", "regular-user-2"),
    "premium": ("premium-user-1",),
    "internal": ("internal-service-1",),
}

# (consumer group, relationship type, target, policy). Target: resource group name,
# (method, path) of a resource, or None for all resources.
# Covers: one policy for ALL resources (free, internal), per resource group (regular),
# per resource (regular POST /carts, premium GET /orders/{item_id}).
# regular has nothing for GET /carts* -> the default policy applies there (MUC3).
ASSIGNMENTS = (
    ("free", RelationshipType.ALL_RESOURCES, None, FREE_POLICY),
    ("regular", RelationshipType.RESOURCE_GROUP, "orders", REGULAR_POLICY),
    ("regular", RelationshipType.RESOURCE_GROUP, "users", REGULAR_POLICY),
    ("regular", RelationshipType.RESOURCE, ("POST", "/carts"), CART_WRITES_POLICY),
    ("premium", RelationshipType.ALL_RESOURCES, None, PREMIUM_POLICY),
    (
        "premium",
        RelationshipType.RESOURCE,
        ("GET", "/orders/{item_id}"),
        ORDER_ITEM_BURST_POLICY,
    ),
    ("internal", RelationshipType.ALL_RESOURCES, None, INTERNAL_POLICY),
)
