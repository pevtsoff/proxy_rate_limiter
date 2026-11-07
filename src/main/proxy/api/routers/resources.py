from typing import Annotated

from fastapi import APIRouter, Depends, Query, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import PaginationDep, ResourceDiscoveryServiceDep, ResourceServiceDep
from proxy.schemas.resource_read import ResourceRead
from proxy.schemas.resource_sync_read import ResourceSyncRead
from proxy.schemas.resource_update import ResourceUpdate

router = APIRouter(
    prefix="/admin/resources",
    tags=["resources"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


@router.post("/sync")
async def sync_resources(service: ResourceDiscoveryServiceDep) -> ResourceSyncRead:
    """Fetch every endpoint and method from the target API swagger docs (MUC1.4)."""
    return ResourceSyncRead.model_validate(await service.synchronize())


@router.get("")
async def list_resources(
    service: ResourceServiceDep,
    page: PaginationDep,
    resource_group_id: Annotated[int | None, Query()] = None,
) -> list[ResourceRead]:
    resources = await service.page(resource_group_id, page.offset, page.limit)
    return [ResourceRead.model_validate(resource) for resource in resources]


@router.get("/{resource_id}")
async def get_resource(resource_id: int, service: ResourceServiceDep) -> ResourceRead:
    return ResourceRead.model_validate(await service.get(resource_id))


@router.patch("/{resource_id}")
async def update_resource(
    resource_id: int, command: ResourceUpdate, service: ResourceServiceDep
) -> ResourceRead:
    return ResourceRead.model_validate(await service.update(resource_id, command))


@router.delete("/{resource_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_resource(resource_id: int, service: ResourceServiceDep) -> None:
    await service.delete(resource_id)
