from proxy.admin_ui.cache_invalidating_view import CacheInvalidatingView
from proxy.db.models.resource_group import ResourceGroup


class ResourceGroupAdmin(CacheInvalidatingView, model=ResourceGroup):
    name_plural = "Resource groups"
    icon = "fa-solid fa-layer-group"
    column_list = [
        ResourceGroup.id,
        ResourceGroup.name,
        ResourceGroup.description,
        ResourceGroup.resources,
    ]
    form_columns = [ResourceGroup.name, ResourceGroup.description, ResourceGroup.resources]
