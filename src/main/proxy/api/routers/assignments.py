from typing import Annotated

from fastapi import APIRouter, Depends, Query, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import AssignmentServiceDep, PaginationDep
from proxy.schemas.assignment_create import AssignmentCreate
from proxy.schemas.assignment_read import AssignmentRead
from proxy.schemas.assignment_update import AssignmentUpdate

router = APIRouter(
    prefix="/admin/consumer-groups-policies",
    tags=["policy assignments"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_assignment(
    command: AssignmentCreate, service: AssignmentServiceDep
) -> AssignmentRead:
    return AssignmentRead.model_validate(await service.create(command))


@router.get("")
async def list_assignments(
    service: AssignmentServiceDep,
    page: PaginationDep,
    consumer_group_id: Annotated[int | None, Query()] = None,
) -> list[AssignmentRead]:
    assignments = await service.page(consumer_group_id, page.offset, page.limit)
    return [AssignmentRead.model_validate(assignment) for assignment in assignments]


@router.get("/{assignment_id}")
async def get_assignment(assignment_id: int, service: AssignmentServiceDep) -> AssignmentRead:
    return AssignmentRead.model_validate(await service.get(assignment_id))


@router.patch("/{assignment_id}")
async def update_assignment(
    assignment_id: int, command: AssignmentUpdate, service: AssignmentServiceDep
) -> AssignmentRead:
    return AssignmentRead.model_validate(await service.update(assignment_id, command))


@router.delete("/{assignment_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_assignment(assignment_id: int, service: AssignmentServiceDep) -> None:
    await service.delete(assignment_id)
