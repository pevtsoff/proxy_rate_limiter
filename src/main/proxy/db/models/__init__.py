"""Importing this package registers every mapped class on `Base.metadata`."""

from proxy.db.models.consumer import Consumer
from proxy.db.models.consumer_group import ConsumerGroup
from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.policy import Policy
from proxy.db.models.resource import Resource
from proxy.db.models.resource_group import ResourceGroup

__all__ = [
    "Consumer",
    "ConsumerGroup",
    "ConsumerGroupPolicy",
    "Policy",
    "Resource",
    "ResourceGroup",
]
