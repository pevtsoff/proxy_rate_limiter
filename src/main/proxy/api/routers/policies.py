from fastapi import APIRouter, Depends, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import PaginationDep, PolicyServiceDep
from proxy.domain.enums.rate_limiting_strategy import DEFAULT_STRATEGY, SUPPORTED_STRATEGIES
from proxy.schemas.policy_create import PolicyCreate
from proxy.schemas.policy_read import PolicyRead
from proxy.schemas.policy_update import PolicyUpdate
from proxy.schemas.strategies_read import StrategiesRead

router = APIRouter(
    prefix="/admin/policies",
    tags=["policies"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


@router.get("/strategies")
async def list_strategies() -> StrategiesRead:
    return StrategiesRead(strategies=list(SUPPORTED_STRATEGIES), default=DEFAULT_STRATEGY.value)


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_policy(command: PolicyCreate, service: PolicyServiceDep) -> PolicyRead:
    return PolicyRead.model_validate(await service.create(command))


@router.get("")
async def list_policies(service: PolicyServiceDep, page: PaginationDep) -> list[PolicyRead]:
    policies = await service.page(page.offset, page.limit)
    return [PolicyRead.model_validate(policy) for policy in policies]


@router.get("/{policy_id}")
async def get_policy(policy_id: int, service: PolicyServiceDep) -> PolicyRead:
    return PolicyRead.model_validate(await service.get(policy_id))


@router.patch("/{policy_id}")
async def update_policy(
    policy_id: int, command: PolicyUpdate, service: PolicyServiceDep
) -> PolicyRead:
    return PolicyRead.model_validate(await service.update(policy_id, command))


@router.delete("/{policy_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_policy(policy_id: int, service: PolicyServiceDep) -> None:
    await service.delete(policy_id)
