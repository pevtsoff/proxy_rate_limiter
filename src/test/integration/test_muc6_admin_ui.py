"""Admin UI model registration and page coverage."""

import pytest
from app.main import admin
from app.models import (
    Consumer,
    ConsumerGroup,
    ConsumerGroupPolicy,
    Policy,
    Resource,
    ResourceGroup,
)
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_muc6_admin_ui_registers_every_configuration_model(api_client: AsyncClient) -> None:
    """SQLAdmin exposes CRUD views for all core configuration tables."""
    registered = {view.model for view in admin._views}

    assert {
        Consumer,
        ConsumerGroup,
        ConsumerGroupPolicy,
        Policy,
        Resource,
        ResourceGroup,
    }.issubset(registered)
    assert all(view.can_create and view.can_edit and view.can_delete for view in admin._views)
    page = await api_client.get("/admin/")
    assert page.status_code == 200
