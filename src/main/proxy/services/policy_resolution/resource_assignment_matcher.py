from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource
from proxy.domain.enums.relationship_type import RelationshipType


class ResourceAssignmentMatcher:
    def matches(self, assignment: ConsumerGroupPolicy, resource: Resource | None) -> bool:
        return (
            resource is not None
            and assignment.relationship_type == RelationshipType.RESOURCE
            and assignment.resource_id == resource.id
        )
