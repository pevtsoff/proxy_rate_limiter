from typing import Any

from starlette.requests import Request

from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.admin_ui.form_values import related_ids
from proxy.db.models.consumer_group import ConsumerGroup
from proxy.domain.errors.domain_validation_error import DomainValidationError


class ConsumerGroupAdmin(CacheInvalidatingView, model=ConsumerGroup):
    name_plural = "Consumer groups"
    icon = "fa-solid fa-users"
    column_list = [ConsumerGroup.id, ConsumerGroup.group_name, ConsumerGroup.consumers]
    form_columns = [ConsumerGroup.group_name, ConsumerGroup.consumers]

    async def on_model_change(
        self, data: dict[str, Any], model: Any, is_created: bool, request: Request
    ) -> None:
        if not related_ids(data.get("consumers")):
            raise DomainValidationError("A consumer group must have at least one consumer")
