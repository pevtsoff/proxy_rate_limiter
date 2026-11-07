import pytest
from pydantic import ValidationError

from proxy.domain.enums.relationship_type import RelationshipType
from proxy.schemas.assignment_create import AssignmentCreate
from proxy.schemas.assignment_update import AssignmentUpdate
from proxy.schemas.consumer_group_create import ConsumerGroupCreate
from proxy.schemas.policy_create import PolicyCreate


def test_consumer_group_needs_at_least_one_consumer() -> None:
    with pytest.raises(ValidationError):
        ConsumerGroupCreate(group_name="free", consumer_ids=[])


def test_consumer_group_rejects_duplicate_consumers() -> None:
    with pytest.raises(ValidationError):
        ConsumerGroupCreate(group_name="free", consumer_ids=["a", "a"])


def test_policy_strategy_defaults_to_moving_window() -> None:
    policy = PolicyCreate(name="p", number_of_requests=1, time_period="minute")
    assert policy.rate_limiting_strategy.value == "moving-window"


def test_assignment_create_rejects_target_of_wrong_type() -> None:
    with pytest.raises(ValidationError):
        AssignmentCreate(
            consumer_group_id=1, relationship_type="resource", policy_id=1, resource_group_id=1
        )


def test_assignment_update_needs_relationship_type_to_change_target() -> None:
    with pytest.raises(ValidationError):
        AssignmentUpdate(resource_id=1)


def test_assignment_update_of_policy_only_keeps_target() -> None:
    update = AssignmentUpdate(policy_id=2)
    assert not update.changes_target()


def test_assignment_update_with_new_target() -> None:
    update = AssignmentUpdate(relationship_type="resource_group", resource_group_id=3)
    assert update.target().relationship_type is RelationshipType.RESOURCE_GROUP
