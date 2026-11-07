"""Common SQLAdmin hooks that invalidate policy lookups after edits."""

from typing import Any

from sqladmin import ModelView
from starlette.requests import Request

from app.services.policy_service import invalidate_policy_cache


class CacheInvalidatingModelView(ModelView):
    """Drop cached decisions whenever an administrator changes configuration."""

    async def after_model_change(self, data: dict[str, Any], model: Any, is_created: bool, request: Request) -> None:
        """Invalidate decisions after admin create or update."""
        await invalidate_policy_cache()

    async def after_model_delete(self, model: Any, request: Request) -> None:
        """Invalidate decisions after admin delete."""
        await invalidate_policy_cache()
