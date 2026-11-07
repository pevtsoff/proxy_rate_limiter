from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.db.models.resource import Resource
from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.enums.io_type import IoType
from proxy.domain.enums.relationship_type import RelationshipType
from proxy.services.policy_resolution.policy_selector import PolicySelector

ORDERS_GROUP_ID = 5


def _resource(resource_id: int = 10, group_id: int | None = ORDERS_GROUP_ID) -> Resource:
    return Resource(
        id=resource_id,
        method=HttpMethod.GET,
        resource_path="/orders/{item_id}",
        io_type=IoType.READ,
        resource_group_id=group_id,
    )


def _assignment(assignment_id: int, relationship_type: RelationshipType, **target: int):
    return ConsumerGroupPolicy(
        id=assignment_id, consumer_group_id=1, relationship_type=relationship_type, **target
    )


ALL = _assignment(1, RelationshipType.ALL_RESOURCES)
GROUP = _assignment(2, RelationshipType.RESOURCE_GROUP, resource_group_id=ORDERS_GROUP_ID)
OWN = _assignment(3, RelationshipType.RESOURCE, resource_id=10)
selector = PolicySelector.with_default_precedence()


def test_resource_assignment_wins_over_group_and_all() -> None:
    assert selector.select([ALL, GROUP, OWN], _resource()) is OWN


def test_group_assignment_wins_over_all_resources() -> None:
    assert selector.select([ALL, GROUP], _resource()) is GROUP


def test_all_resources_assignment_is_the_fallback() -> None:
    assert selector.select([ALL, GROUP], _resource(group_id=None)) is ALL


def test_unknown_resource_only_matches_all_resources() -> None:
    assert selector.select([GROUP, OWN, ALL], None) is ALL


def test_no_match_means_default_policy() -> None:
    other_resource = _assignment(4, RelationshipType.RESOURCE, resource_id=99)
    assert selector.select([other_resource], _resource()) is None
