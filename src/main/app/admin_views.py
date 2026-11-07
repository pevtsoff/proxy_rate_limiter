"""SQLAdmin views and registration for configuration models."""

from typing import Any, ClassVar

from fastapi import FastAPI
from sqladmin import Admin, ModelView
from sqlalchemy.ext.asyncio import AsyncEngine
from starlette.requests import Request

from app.models import (
    Consumer,
    ConsumerGroup,
    ConsumerGroupPolicy,
    Policy,
    Resource,
    ResourceGroup,
    ServiceSetting,
)
from app.services.policy_service import invalidate_policy_cache


class CacheInvalidatingModelView(ModelView):
    """Drop cached decisions whenever an administrator changes configuration."""

    async def after_model_change(
        self, data: dict[str, Any], model: Any, is_created: bool, request: Request
    ) -> None:
        """Invalidate decisions after admin create or update."""
        await invalidate_policy_cache()

    async def after_model_delete(self, model: Any, request: Request) -> None:
        """Invalidate decisions after admin delete."""
        await invalidate_policy_cache()


class PolicyAdmin(CacheInvalidatingModelView, model=Policy):
    column_list: ClassVar[list[object]] = [
        Policy.id,
        Policy.name,
        Policy.number_of_requests,
        Policy.time_period,
    ]


class ConsumerGroupAdmin(CacheInvalidatingModelView, model=ConsumerGroup):
    column_list: ClassVar[list[object]] = [ConsumerGroup.id, ConsumerGroup.group_name]


class ConsumerAdmin(CacheInvalidatingModelView, model=Consumer):
    column_list: ClassVar[list[object]] = [Consumer.id, Consumer.consumer_group_id]


class ResourceGroupAdmin(CacheInvalidatingModelView, model=ResourceGroup):
    column_list: ClassVar[list[object]] = [ResourceGroup.id, ResourceGroup.name]


class ResourceAdmin(CacheInvalidatingModelView, model=Resource):
    column_list: ClassVar[list[object]] = [
        Resource.id,
        Resource.method,
        Resource.resource_path,
        Resource.io_type,
    ]


class AssignmentAdmin(CacheInvalidatingModelView, model=ConsumerGroupPolicy):
    column_list: ClassVar[list[object]] = [
        ConsumerGroupPolicy.id,
        ConsumerGroupPolicy.consumer_group_id,
        ConsumerGroupPolicy.policy_id,
    ]


class SettingsAdmin(CacheInvalidatingModelView, model=ServiceSetting):
    column_list: ClassVar[list[object]] = [
        ServiceSetting.id,
        ServiceSetting.key,
        ServiceSetting.value,
    ]


ADMIN_VIEWS: tuple[type[ModelView], ...] = (
    PolicyAdmin,
    ConsumerGroupAdmin,
    ConsumerAdmin,
    ResourceGroupAdmin,
    ResourceAdmin,
    AssignmentAdmin,
    SettingsAdmin,
)


def configure_admin(app: FastAPI, engine: AsyncEngine) -> Admin:
    """Attach SQLAdmin and all configuration model views to the application."""
    admin = Admin(app, engine, base_url="/admin", title="Rate Limiter Admin")
    register_admin_views(admin)
    return admin


def register_admin_views(admin: Admin) -> None:
    """Register every configuration model with SQLAdmin."""
    for view in ADMIN_VIEWS:
        admin.add_view(view)
