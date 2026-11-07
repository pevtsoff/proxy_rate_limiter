from proxy.db.models.consumer import Consumer
from proxy.db.models.resource import Resource
from proxy.domain.errors.unknown_consumer_error import UnknownConsumerError
from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.domain.value_objects.resolved_policy import ResolvedPolicy
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.services.policy_resolution.policy_selector import PolicySelector
from proxy.services.policy_resolution.resource_locator import ResourceLocator

UNMATCHED_SCOPE = "default:unmatched"


class DatabasePolicyResolver:
    """Reads the proxy database to find the policy for (consumer, method + path).

    Falls back to the default policy when the consumer group has no matching assignment
    (MUC3). Unknown paths share one default bucket so varying the path cannot dodge limits.
    """

    def __init__(self, uow: SqlAlchemyUnitOfWork, selector: PolicySelector) -> None:
        self._uow = uow
        self._selector = selector

    async def resolve(self, consumer_id: ConsumerId, target: RequestTarget) -> ResolvedPolicy:
        consumer = await self._find_consumer(consumer_id, target)
        resource = await ResourceLocator(self._uow.resources).locate(target)
        assignments = await self._uow.assignments.list_for_consumer_group(
            consumer.consumer_group_id
        )
        assignment = self._selector.select(assignments, resource)
        if assignment is None:
            return await self._default_policy(resource)
        policy = assignment.policy
        return ResolvedPolicy(policy.name, policy.to_rule(), assignment.scope_key())

    async def _find_consumer(self, consumer_id: ConsumerId, target: RequestTarget) -> Consumer:
        consumer = await self._uow.consumers.get(consumer_id.value)
        if consumer is None:
            raise UnknownConsumerError(
                f"Consumer '{consumer_id}' is not registered", target.resource_label
            )
        return consumer

    async def _default_policy(self, resource: Resource | None) -> ResolvedPolicy:
        policy = await self._uow.policies.get_default()
        if policy is None:
            raise LookupError("The default policy is missing; run the bootstrap step")
        scope = UNMATCHED_SCOPE if resource is None else f"default:resource:{resource.id}"
        return ResolvedPolicy(policy.name, policy.to_rule(), scope)
