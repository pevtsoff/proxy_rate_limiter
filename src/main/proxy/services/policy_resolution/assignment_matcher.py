from typing import Protocol

from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource


class AssignmentMatcher(Protocol):
    """Decides whether an assignment covers the requested resource (None = unknown path)."""

    def matches(self, assignment: ConsumerGroupPolicy, resource: Resource | None) -> bool: ...
