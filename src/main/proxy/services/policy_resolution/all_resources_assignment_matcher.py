from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource
from proxy.domain.enums.relationship_type import RelationshipType


class AllResourcesAssignmentMatcher:
    def matches(self, assignment: ConsumerGroupPolicy, resource: Resource | None) -> bool:
        return assignment.relationship_type == RelationshipType.ALL_RESOURCES
