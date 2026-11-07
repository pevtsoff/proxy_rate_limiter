"""Policy management endpoints."""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from limits.strategies import STRATEGIES
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_session
from app.models.entities import ConsumerGroup, ConsumerGroupPolicy, Policy, Resource, ResourceGroup
from app.services.policy_service import invalidate_policy_cache

router = APIRouter(prefix="/admin-api", tags=["administration"])
SessionDep = Annotated[AsyncSession, Depends(get_session)]


class PolicyInput(BaseModel):
    """Validated policy payload."""

    name: str = Field(min_length=1, max_length=100)
    rate_limiting_strategy: str = "moving-window"
    number_of_requests: int = Field(gt=0)
    time_period: str


class AssignmentInput(BaseModel):
    """Validated assignment payload."""

    consumer_group_id: int
    relationship_type: str
    policy_id: int
    resource_id: int


@router.get("/policies")
async def get_policies(session: SessionDep) -> list[dict[str, int | str]]:
    """Return configured policies."""
    policies = (await session.scalars(select(Policy))).all()
    return [{"id": p.id, "name": p.name, "rate_limiting_strategy": p.rate_limiting_strategy, "number_of_requests": p.number_of_requests, "time_period": p.time_period} for p in policies]


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
async def update_policy(policy_id: int, data: PolicyInput, session: SessionDep) -> dict[str, int | str]:
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
    """Assign one policy consistently to a resource or resource group."""
    if data.relationship_type not in {"resource", "resource_group"}:
        raise HTTPException(422, "relationship_type must be resource or resource_group")
    if await session.get(ConsumerGroup, data.consumer_group_id) is None or await session.get(Policy, data.policy_id) is None:
        raise HTTPException(404, "Consumer group or policy not found")
    model = Resource if data.relationship_type == "resource" else ResourceGroup
    if await session.get(model, data.resource_id) is None:
        raise HTTPException(404, "Resource not found")
    old = (await session.scalars(select(ConsumerGroupPolicy).where(ConsumerGroupPolicy.consumer_group_id == data.consumer_group_id))).all()
    if old and any(rule.relationship_type != data.relationship_type for rule in old):
        raise HTTPException(409, "A consumer group cannot mix resource and resource_group assignments")
    if data.relationship_type == "resource":
        resource = await session.get(Resource, data.resource_id)
        assigned_groups = {rule.resource_id for rule in old if rule.relationship_type == "resource_group"}
        if resource and resource.resource_group_id in assigned_groups:
            raise HTTPException(409, "Resource is already covered by an assigned resource group")
    assignment = ConsumerGroupPolicy(**data.model_dump())
    session.add(assignment)
    await session.commit()
    await session.refresh(assignment)
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
async def update_assignment(assignment_id: int, data: AssignmentInput, session: SessionDep) -> dict[str, int | str]:
    """Replace a policy assignment while preserving group consistency."""
    assignment = await session.get(ConsumerGroupPolicy, assignment_id)
    if assignment is None:
        raise HTTPException(404, "Assignment not found")
    group_rules = (await session.scalars(select(ConsumerGroupPolicy).where(
        ConsumerGroupPolicy.consumer_group_id == data.consumer_group_id,
        ConsumerGroupPolicy.id != assignment_id,
    ))).all()
    if any(rule.relationship_type != data.relationship_type for rule in group_rules):
        raise HTTPException(409, "A consumer group cannot mix resource and resource_group assignments")
    for key, value in data.model_dump().items():
        setattr(assignment, key, value)
    await session.commit()
    await invalidate_policy_cache()
    return {"id": assignment.id, **data.model_dump()}
