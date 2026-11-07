from typing import Annotated

from fastapi import APIRouter, Depends, Query, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import ConsumerServiceDep, PaginationDep
from proxy.schemas.consumer_create import ConsumerCreate
from proxy.schemas.consumer_read import ConsumerRead
from proxy.schemas.consumer_update import ConsumerUpdate

router = APIRouter(
    prefix="/admin/consumers",
    tags=["consumers"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_consumer(command: ConsumerCreate, service: ConsumerServiceDep) -> ConsumerRead:
    return ConsumerRead.model_validate(await service.create(command))


@router.get("")
async def list_consumers(
    service: ConsumerServiceDep,
    page: PaginationDep,
    consumer_group_id: Annotated[int | None, Query()] = None,
) -> list[ConsumerRead]:
    consumers = await service.page(consumer_group_id, page.offset, page.limit)
    return [ConsumerRead.model_validate(consumer) for consumer in consumers]


@router.get("/{consumer_id}")
async def get_consumer(consumer_id: str, service: ConsumerServiceDep) -> ConsumerRead:
    return ConsumerRead.model_validate(await service.get(consumer_id))


@router.patch("/{consumer_id}")
async def update_consumer(
    consumer_id: str, command: ConsumerUpdate, service: ConsumerServiceDep
) -> ConsumerRead:
    return ConsumerRead.model_validate(await service.update(consumer_id, command))


@router.delete("/{consumer_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_consumer(consumer_id: str, service: ConsumerServiceDep) -> None:
    await service.delete(consumer_id)
