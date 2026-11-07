from dataclasses import dataclass

from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.errors.domain_validation_error import DomainValidationError


@dataclass(frozen=True, slots=True)
class AssignmentTarget:
    """What a consumer group policy assignment points to.

    Exactly the id matching `relationship_type` must be set.
    """

    relationship_type: RelationshipType
    resource_id: int | None = None
    resource_group_id: int | None = None

    def __post_init__(self) -> None:
        expected = (
            self.relationship_type is RelationshipType.RESOURCE,
            self.relationship_type is RelationshipType.RESOURCE_GROUP,
        )
        actual = (self.resource_id is not None, self.resource_group_id is not None)
        if expected != actual:
            raise DomainValidationError(self._mismatch_message())

    def _mismatch_message(self) -> str:
        if self.relationship_type is RelationshipType.RESOURCE:
            return "A 'resource' assignment needs resource_id and no resource_group_id"
        if self.relationship_type is RelationshipType.RESOURCE_GROUP:
            return "A 'resource_group' assignment needs resource_group_id and no resource_id"
        return "An 'all_resources' assignment takes neither resource_id nor resource_group_id"
