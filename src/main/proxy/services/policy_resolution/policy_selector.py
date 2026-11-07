from collections.abc import Sequence

from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource
from proxy.services.policy_resolution.all_resources_assignment_matcher import (
    AllResourcesAssignmentMatcher,
)
from proxy.services.policy_resolution.assignment_matcher import AssignmentMatcher
from proxy.services.policy_resolution.resource_assignment_matcher import (
    ResourceAssignmentMatcher,
)
from proxy.services.policy_resolution.resource_group_assignment_matcher import (
    ResourceGroupAssignmentMatcher,
)


class PolicySelector:
    """Picks the assignment for a resource using matchers in precedence order.

    New kinds of assignment are added by adding a matcher, not by editing this class.
    """

    def __init__(self, matchers: Sequence[AssignmentMatcher]) -> None:
        self._matchers = tuple(matchers)

    @classmethod
    def with_default_precedence(cls) -> "PolicySelector":
        return cls(
            [
                ResourceAssignmentMatcher(),
                ResourceGroupAssignmentMatcher(),
                AllResourcesAssignmentMatcher(),
            ]
        )

    def select(
        self, assignments: Sequence[ConsumerGroupPolicy], resource: Resource | None
    ) -> ConsumerGroupPolicy | None:
        for matcher in self._matchers:
            matched = next((a for a in assignments if matcher.matches(a, resource)), None)
            if matched is not None:
                return matched
        return None
