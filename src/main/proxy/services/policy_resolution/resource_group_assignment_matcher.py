from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource
from proxy.domain.enums.relationship_type import RelationshipType


class ResourceGroupAssignmentMatcher:
    def matches(self, assignment: ConsumerGroupPolicy, resource: Resource | None) -> bool:
        return (
            resource is not None
            and resource.resource_group_id is not None
            and assignment.relationship_type == RelationshipType.RESOURCE_GROUP
            and assignment.resource_group_id == resource.resource_group_id
        )
