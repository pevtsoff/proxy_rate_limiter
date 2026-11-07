from fastapi import APIRouter, Depends, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import PaginationDep, ResourceGroupServiceDep
from proxy.db.models.resource_group import ResourceGroup
from proxy.schemas.resource_group_create import ResourceGroupCreate
from proxy.schemas.resource_group_read import ResourceGroupRead
from proxy.schemas.resource_group_update import ResourceGroupUpdate

router = APIRouter(
    prefix="/admin/resource-groups",
    tags=["resource groups"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


def _to_read(group: ResourceGroup) -> ResourceGroupRead:
    return ResourceGroupRead(
        id=group.id,
        name=group.name,
        description=group.description,
        resource_ids=sorted(resource.id for resource in group.resources),
    )


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_resource_group(
    command: ResourceGroupCreate, service: ResourceGroupServiceDep
) -> ResourceGroupRead:
    return _to_read(await service.create(command))


@router.get("")
async def list_resource_groups(
    service: ResourceGroupServiceDep, page: PaginationDep
) -> list[ResourceGroupRead]:
    return [_to_read(group) for group in await service.page(page.offset, page.limit)]


@router.get("/{resource_group_id}")
async def get_resource_group(
    resource_group_id: int, service: ResourceGroupServiceDep
) -> ResourceGroupRead:
    return _to_read(await service.get(resource_group_id))


@router.patch("/{resource_group_id}")
async def update_resource_group(
    resource_group_id: int, command: ResourceGroupUpdate, service: ResourceGroupServiceDep
) -> ResourceGroupRead:
    return _to_read(await service.update(resource_group_id, command))


@router.delete("/{resource_group_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_resource_group(resource_group_id: int, service: ResourceGroupServiceDep) -> None:
    await service.delete(resource_group_id)
