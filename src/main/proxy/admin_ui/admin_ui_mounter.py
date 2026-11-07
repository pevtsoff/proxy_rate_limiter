from fastapi import FastAPI
from sqladmin import Admin

from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.admin_ui.consumer_admin import ConsumerAdmin
from proxy.admin_ui.consumer_group_admin import ConsumerGroupAdmin
from proxy.admin_ui.consumer_group_policy_admin import ConsumerGroupPolicyAdmin
from proxy.admin_ui.policy_admin import PolicyAdmin
from proxy.admin_ui.resource_admin import ResourceAdmin
from proxy.admin_ui.resource_group_admin import ResourceGroupAdmin
from proxy.api.app_container import AppContainer

ADMIN_UI_PATH = "/admin-ui"
ADMIN_VIEWS: tuple[type[CacheInvalidatingView], ...] = (
    ConsumerGroupAdmin,
    ConsumerAdmin,
    PolicyAdmin,
    ResourceGroupAdmin,
    ResourceAdmin,
    ConsumerGroupPolicyAdmin,
)


def bind_view(
    view: type[CacheInvalidatingView], container: AppContainer
) -> type[CacheInvalidatingView]:
    """Subclass a view with this application's container (sqladmin instantiates views)."""
    return type(view.__name__, (view,), {"container": container})


def mount_admin_ui(app: FastAPI, container: AppContainer) -> Admin:
    admin = Admin(
        app,
        session_maker=container.session_factory,
        base_url=ADMIN_UI_PATH,
        title="Proxy Rate Limiter Admin",
        authentication_backend=None,
    )
    for view in ADMIN_VIEWS:
        admin.add_view(bind_view(view, container))
    return admin
