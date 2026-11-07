"""Use cases for assigning policies to consumer-group resource scopes."""

from app.models import ConsumerGroup, ConsumerGroupPolicy, Policy, Resource, ResourceGroup
from app.services.errors.assignment_conflict_error import AssignmentConflictError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

RELATIONSHIP_MODELS = {"resource": Resource, "resource_group": ResourceGroup}


async def create_assignment(
    session: AsyncSession,
    consumer_group_id: int,
    relationship_type: str,
    policy_id: int,
    resource_id: int,
) -> ConsumerGroupPolicy:
    """Validate and persist a new assignment."""
    await validate_references(session, consumer_group_id, relationship_type, policy_id, resource_id)
    existing = await load_group_assignments(session, consumer_group_id)
    await validate_scope(session, existing, relationship_type, resource_id)
    assignment = ConsumerGroupPolicy(
        consumer_group_id=consumer_group_id,
        relationship_type=relationship_type,
        policy_id=policy_id,
        resource_id=resource_id,
    )
    session.add(assignment)
    await session.commit()
    await session.refresh(assignment)
    return assignment


async def update_assignment(
    session: AsyncSession,
    assignment_id: int,
    consumer_group_id: int,
    relationship_type: str,
    policy_id: int,
    resource_id: int,
) -> ConsumerGroupPolicy:
    """Validate and replace an assignment while excluding its former scope."""
    assignment = await session.get(ConsumerGroupPolicy, assignment_id)
    if assignment is None:
        raise LookupError("Assignment not found")
    await validate_references(session, consumer_group_id, relationship_type, policy_id, resource_id)
    other_rules = await load_group_assignments(
        session, consumer_group_id, excluding_id=assignment_id
    )
    await validate_scope(session, other_rules, relationship_type, resource_id)
    assign_values(assignment, consumer_group_id, relationship_type, policy_id, resource_id)
    await session.commit()
    await session.refresh(assignment)
    return assignment


async def validate_references(
    session: AsyncSession,
    consumer_group_id: int,
    relationship_type: str,
    policy_id: int,
    resource_id: int,
) -> None:
    model = RELATIONSHIP_MODELS.get(relationship_type)
    if model is None:
        raise ValueError("relationship_type must be resource or resource_group")
    if await session.get(ConsumerGroup, consumer_group_id) is None:
        raise LookupError("Consumer group not found")
    if await session.get(Policy, policy_id) is None:
        raise LookupError("Policy not found")
    if await session.get(model, resource_id) is None:
        raise LookupError("Resource or resource group not found")


async def load_group_assignments(
    session: AsyncSession, consumer_group_id: int, excluding_id: int | None = None
) -> list[ConsumerGroupPolicy]:
    query = select(ConsumerGroupPolicy).where(
        ConsumerGroupPolicy.consumer_group_id == consumer_group_id
    )
    if excluding_id is not None:
        query = query.where(ConsumerGroupPolicy.id != excluding_id)
    return list((await session.scalars(query)).all())


async def validate_scope(
    session: AsyncSession,
    existing: list[ConsumerGroupPolicy],
    relationship_type: str,
    resource_id: int,
) -> None:
    if any(rule.relationship_type != relationship_type for rule in existing):
        raise AssignmentConflictError("A consumer group cannot mix assignment scopes")
    if relationship_type == "resource_group":
        await reject_groups_covering_assigned_resources(session, existing, resource_id)
        return
    await reject_resource_covered_by_assigned_group(session, existing, resource_id)


async def reject_groups_covering_assigned_resources(
    session: AsyncSession, existing: list[ConsumerGroupPolicy], resource_group_id: int
) -> None:
    resource_ids = [rule.resource_id for rule in existing if rule.relationship_type == "resource"]
    if not resource_ids:
        return
    resource = await session.scalar(
        select(Resource.id).where(
            Resource.id.in_(resource_ids), Resource.resource_group_id == resource_group_id
        )
    )
    if resource is not None:
        raise AssignmentConflictError("Resource group already covers assigned resources")


async def reject_resource_covered_by_assigned_group(
    session: AsyncSession, existing: list[ConsumerGroupPolicy], resource_id: int
) -> None:
    resource = await session.get(Resource, resource_id)
    group_ids = [
        rule.resource_id for rule in existing if rule.relationship_type == "resource_group"
    ]
    if resource and resource.resource_group_id in group_ids:
        raise AssignmentConflictError("Resource is already covered by an assigned resource group")


def assign_values(
    assignment: ConsumerGroupPolicy,
    consumer_group_id: int,
    relationship_type: str,
    policy_id: int,
    resource_id: int,
) -> None:
    assignment.consumer_group_id = consumer_group_id
    assignment.relationship_type = relationship_type
    assignment.policy_id = policy_id
    assignment.resource_id = resource_id
