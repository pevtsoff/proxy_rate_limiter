from typing import Any

from starlette.requests import Request

from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.admin_ui.form_values import related_id
from proxy.db.models.consumer import Consumer
from proxy.domain.errors.domain_validation_error import DomainValidationError
from proxy.services.consumer_membership_guard import ConsumerMembershipGuard


class ConsumerAdmin(CacheInvalidatingView, model=Consumer):
    name_plural = "Consumers"
    icon = "fa-solid fa-user"
    form_include_pk = True
    column_list = [Consumer.id, Consumer.consumer_group]
    form_columns = [Consumer.id, Consumer.consumer_group]
    column_searchable_list = [Consumer.id]

    async def on_model_change(
        self, data: dict[str, Any], model: Any, is_created: bool, request: Request
    ) -> None:
        new_group_id = related_id(data.get("consumer_group"))
        if new_group_id is None:
            raise DomainValidationError("A consumer must belong to a consumer group")
        if not is_created and new_group_id != model.consumer_group_id:
            await self._ensure_can_leave_group(model)

    async def on_model_delete(self, model: Any, request: Request) -> None:
        await self._ensure_can_leave_group(model)

    async def _ensure_can_leave_group(self, consumer: Consumer) -> None:
        async with self.unit_of_work() as uow:
            await ConsumerMembershipGuard(uow.consumers).ensure_can_leave_group(consumer)
