"""Compatibility exports for the rate limiter database models."""

from app.models.consumer import Consumer
from app.models.consumer_group import ConsumerGroup
from app.models.consumer_group_policy import ConsumerGroupPolicy
from app.models.policy import Policy
from app.models.resource import Resource
from app.models.resource_group import ResourceGroup
from app.models.service_setting import ServiceSetting

__all__ = [
    "Consumer",
    "ConsumerGroup",
    "ConsumerGroupPolicy",
    "Policy",
    "Resource",
    "ResourceGroup",
    "ServiceSetting",
]
