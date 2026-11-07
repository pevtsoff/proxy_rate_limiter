"""Policy management endpoints."""

from typing import Annotated

from app.database import get_session
from app.models import ConsumerGroupPolicy, Policy
from app.schemas.assignment_input import AssignmentInput
from app.schemas.policy_input import PolicyInput
from app.services.assignment_service import create_assignment
from app.services.assignment_service import update_assignment as replace_assignment
from app.services.errors.assignment_conflict_error import AssignmentConflictError
from app.services.policy_service import invalidate_policy_cache
from fastapi import APIRouter, Depends, HTTPException
from limits.strategies import STRATEGIES
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

router = APIRouter(prefix="/admin-api", tags=["administration"])
SessionDep = Annotated[AsyncSession, Depends(get_session)]


@router.get("/policies")
async def get_policies(session: SessionDep) -> list[dict[str, int | str]]:
    """Return configured policies."""
    policies = (await session.scalars(select(Policy))).all()
    return [
        {
            "id": p.id,
            "name": p.name,
            "rate_limiting_strategy": p.rate_limiting_strategy,
            "number_of_requests": p.number_of_requests,
            "time_period": p.time_period,
        }
        for p in policies
    ]


@router.post("/policies", status_code=201)
async def create_policy(data: PolicyInput, session: SessionDep) -> dict[str, int | str]:
    """Create a rate policy."""
    if data.time_period not in {"second", "minute", "hour", "day", "week"}:
        raise HTTPException(422, "Unsupported time period")
    if data.rate_limiting_strategy not in STRATEGIES:
        raise HTTPException(422, "Unsupported rate limiting strategy")
    policy = Policy(**data.model_dump())
    session.add(policy)
    await session.commit()
    await session.refresh(policy)
    await invalidate_policy_cache()
    return {"id": policy.id, **data.model_dump()}


@router.put("/policies/{policy_id}")
async def update_policy(
    policy_id: int, data: PolicyInput, session: SessionDep
) -> dict[str, int | str]:
    """Replace a rate policy."""
    policy = await session.get(Policy, policy_id)
    if policy is None:
        raise HTTPException(404, "Policy not found")
    for key, value in data.model_dump().items():
        setattr(policy, key, value)
    await session.commit()
    await invalidate_policy_cache()
    return {"id": policy.id, **data.model_dump()}


@router.delete("/policies/{policy_id}", status_code=204)
async def delete_policy(policy_id: int, session: SessionDep) -> None:
    """Delete a rate policy."""
    policy = await session.get(Policy, policy_id)
    if policy is None:
        raise HTTPException(404, "Policy not found")
    await session.delete(policy)
    await session.commit()
    await invalidate_policy_cache()


@router.post("/assignments", status_code=201)
async def assign_policy(data: AssignmentInput, session: SessionDep) -> dict[str, int | str]:
    """Create a policy assignment after validating its target scope."""
    try:
        assignment = await create_assignment(session, **data.model_dump())
    except AssignmentConflictError as error:
        raise HTTPException(409, str(error)) from error
    except ValueError as error:
        raise HTTPException(422, str(error)) from error
    except LookupError as error:
        raise HTTPException(404, str(error)) from error
    await invalidate_policy_cache()
    return {"id": assignment.id, **data.model_dump()}


@router.delete("/assignments/{assignment_id}", status_code=204)
async def delete_assignment(assignment_id: int, session: SessionDep) -> None:
    """Delete a policy assignment."""
    assignment = await session.get(ConsumerGroupPolicy, assignment_id)
    if assignment is None:
        raise HTTPException(404, "Assignment not found")
    await session.delete(assignment)
    await session.commit()
    await invalidate_policy_cache()


@router.put("/assignments/{assignment_id}")
async def update_assignment(
    assignment_id: int, data: AssignmentInput, session: SessionDep
) -> dict[str, int | str]:
    """Replace an assignment using the same checks as assignment creation."""
    try:
        assignment = await replace_assignment(session, assignment_id, **data.model_dump())
    except AssignmentConflictError as error:
        raise HTTPException(409, str(error)) from error
    except ValueError as error:
        raise HTTPException(422, str(error)) from error
    except LookupError as error:
        raise HTTPException(404, str(error)) from error
    await invalidate_policy_cache()
    return {"id": assignment.id, **data.model_dump()}
