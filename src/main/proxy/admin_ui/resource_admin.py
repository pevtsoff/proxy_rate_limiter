from typing import Any

from starlette.requests import Request

from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.admin_ui.form_values import related_id
from proxy.db.models.resource import Resource
from proxy.services.assignment_conflict_checker import AssignmentConflictChecker


class ResourceAdmin(CacheInvalidatingView, model=Resource):
    name_plural = "Resources"
    icon = "fa-solid fa-route"
    column_list = [
        Resource.id,
        Resource.method,
        Resource.resource_path,
        Resource.io_type,
        Resource.resource_group,
    ]
    form_columns = [
        Resource.method,
        Resource.resource_path,
        Resource.io_type,
        Resource.resource_group,
    ]
    column_searchable_list = [Resource.resource_path]

    async def on_model_change(
        self, data: dict[str, Any], model: Any, is_created: bool, request: Request
    ) -> None:
        new_group_id = related_id(data.get("resource_group"))
        if is_created or new_group_id == model.resource_group_id:
            return
        async with self.unit_of_work() as uow:
            checker = AssignmentConflictChecker(uow)
            await checker.ensure_resource_can_join_group(model.id, new_group_id)
