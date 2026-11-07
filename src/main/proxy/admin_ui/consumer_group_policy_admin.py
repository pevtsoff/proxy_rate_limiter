from typing import Any

from starlette.requests import Request

from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.admin_ui.form_values import related_id
from proxy.db.models.consumer_group_policy import ConsumerGroupPolicy
from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.errors.domain_validation_error import DomainValidationError
from proxy.domain.value_objects.assignment_target import AssignmentTarget
from proxy.services.assignment_conflict_checker import AssignmentConflictChecker


class ConsumerGroupPolicyAdmin(CacheInvalidatingView, model=ConsumerGroupPolicy):
    """Policy assignments; validated with the same conflict rules as the REST API."""

    name = "Policy assignment"
    name_plural = "Policy assignments"
    icon = "fa-solid fa-link"
    column_list = [
        ConsumerGroupPolicy.id,
        ConsumerGroupPolicy.consumer_group,
        ConsumerGroupPolicy.relationship_type,
        ConsumerGroupPolicy.policy,
        ConsumerGroupPolicy.resource,
        ConsumerGroupPolicy.resource_group,
    ]
    form_columns = [
        ConsumerGroupPolicy.consumer_group,
        ConsumerGroupPolicy.relationship_type,
        ConsumerGroupPolicy.policy,
        ConsumerGroupPolicy.resource,
        ConsumerGroupPolicy.resource_group,
    ]

    async def on_model_change(
        self, data: dict[str, Any], model: Any, is_created: bool, request: Request
    ) -> None:
        consumer_group_id = related_id(data.get("consumer_group"))
        if consumer_group_id is None or related_id(data.get("policy")) is None:
            raise DomainValidationError("Consumer group and policy are required")
        target = self._target(data)
        exclude_id = None if is_created else model.id
        async with self.unit_of_work() as uow:
            checker = AssignmentConflictChecker(uow)
            await checker.ensure_assignable(consumer_group_id, target, exclude_id)

    @staticmethod
    def _target(data: dict[str, Any]) -> AssignmentTarget:
        return AssignmentTarget(
            RelationshipType(data.get("relationship_type")),
            related_id(data.get("resource")),
            related_id(data.get("resource_group")),
        )
