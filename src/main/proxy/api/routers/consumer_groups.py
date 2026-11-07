from fastapi import APIRouter, Depends, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import ConsumerGroupServiceDep, PaginationDep
from proxy.schemas.consumer_group_create import ConsumerGroupCreate
from proxy.schemas.consumer_group_read import ConsumerGroupRead
from proxy.schemas.consumer_group_update import ConsumerGroupUpdate

router = APIRouter(
    prefix="/admin/consumer-groups",
    tags=["consumer groups"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_consumer_group(
    command: ConsumerGroupCreate, service: ConsumerGroupServiceDep
) -> ConsumerGroupRead:
    return ConsumerGroupRead.model_validate(await service.create(command))


@router.get("")
async def list_consumer_groups(
    service: ConsumerGroupServiceDep, page: PaginationDep
) -> list[ConsumerGroupRead]:
    groups = await service.page(page.offset, page.limit)
    return [ConsumerGroupRead.model_validate(group) for group in groups]


@router.get("/{consumer_group_id}")
async def get_consumer_group(
    consumer_group_id: int, service: ConsumerGroupServiceDep
) -> ConsumerGroupRead:
    return ConsumerGroupRead.model_validate(await service.get(consumer_group_id))


@router.patch("/{consumer_group_id}")
async def update_consumer_group(
    consumer_group_id: int, command: ConsumerGroupUpdate, service: ConsumerGroupServiceDep
) -> ConsumerGroupRead:
    return ConsumerGroupRead.model_validate(await service.update(consumer_group_id, command))


@router.delete("/{consumer_group_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_consumer_group(consumer_group_id: int, service: ConsumerGroupServiceDep) -> None:
    await service.delete(consumer_group_id)
