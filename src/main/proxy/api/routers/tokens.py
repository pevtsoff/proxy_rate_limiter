from fastapi import APIRouter, Depends, status

from proxy.api.admin_rate_limit import enforce_admin_rate_limit
from proxy.api.dependencies import TokenServiceDep
from proxy.schemas.token_create import TokenCreate
from proxy.schemas.token_read import TokenRead

router = APIRouter(
    prefix="/admin/tokens",
    tags=["tokens"],
    dependencies=[Depends(enforce_admin_rate_limit)],
)


@router.post("", status_code=status.HTTP_201_CREATED)
async def issue_token(command: TokenCreate, service: TokenServiceDep) -> TokenRead:
    """Development helper: mint a bearer token for a registered consumer."""
    return await service.issue(command)
