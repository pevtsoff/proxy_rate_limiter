from typing import Any

from starlette.requests import Request

from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.db.models.policy import Policy
from proxy.domain.errors.conflict_error import ConflictError


class PolicyAdmin(CacheInvalidatingView, model=Policy):
    name_plural = "Policies"
    icon = "fa-solid fa-gauge"
    column_list = [
        Policy.id,
        Policy.name,
        Policy.rate_limiting_strategy,
        Policy.number_of_requests,
        Policy.time_period,
        Policy.is_default,
    ]
    form_columns = [
        Policy.name,
        Policy.rate_limiting_strategy,
        Policy.number_of_requests,
        Policy.time_period,
    ]
    column_searchable_list = [Policy.name]

    async def on_model_delete(self, model: Any, request: Request) -> None:
        if model.is_default:
            raise ConflictError("The default policy cannot be deleted")
        async with self.unit_of_work() as uow:
            if await uow.assignments.count_using_policy(model.id):
                raise ConflictError(f"Policy '{model.name}' is still assigned")
