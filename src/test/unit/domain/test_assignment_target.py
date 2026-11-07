import pytest

from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.errors.domain_validation_error import DomainValidationError
from proxy.domain.value_objects.assignment_target import AssignmentTarget


@pytest.mark.parametrize(
    ("relationship_type", "resource_id", "resource_group_id"),
    [
        (RelationshipType.RESOURCE, 1, None),
        (RelationshipType.RESOURCE_GROUP, None, 1),
        (RelationshipType.ALL_RESOURCES, None, None),
    ],
)
def test_valid_targets(
    relationship_type: RelationshipType, resource_id: int | None, resource_group_id: int | None
) -> None:
    AssignmentTarget(relationship_type, resource_id, resource_group_id)


@pytest.mark.parametrize(
    ("relationship_type", "resource_id", "resource_group_id"),
    [
        (RelationshipType.RESOURCE, None, None),
        (RelationshipType.RESOURCE, 1, 1),
        (RelationshipType.RESOURCE_GROUP, 1, None),
        (RelationshipType.ALL_RESOURCES, 1, None),
        (RelationshipType.ALL_RESOURCES, None, 1),
    ],
)
def test_target_must_match_relationship_type(
    relationship_type: RelationshipType, resource_id: int | None, resource_group_id: int | None
) -> None:
    with pytest.raises(DomainValidationError):
        AssignmentTarget(relationship_type, resource_id, resource_group_id)
