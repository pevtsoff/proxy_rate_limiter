from proxy.config.settings import Settings
from proxy.domain.enums.consumer_group_name import ConsumerGroupName
from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.enums.rate_limiting_strategy import RateLimitingStrategy
from proxy.domain.enums.relationship_type import RelationshipType
from proxy.domain.errors.not_found_error import NotFoundError
from proxy.ports.api_spec_source import ApiSpecSource
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.schemas.assignment_create import AssignmentCreate
from proxy.schemas.consumer_group_create import ConsumerGroupCreate
from proxy.schemas.policy_create import PolicyCreate
from proxy.schemas.resource_group_create import ResourceGroupCreate
from proxy.seeding import seed_plan
from proxy.seeding.default_policy_bootstrapper import DefaultPolicyBootstrapper
from proxy.services.assignment_service import AssignmentService
from proxy.services.consumer_group_service import ConsumerGroupService
from proxy.services.policy_service import PolicyService
from proxy.services.resource_discovery_service import ResourceDiscoveryService
from proxy.services.resource_group_service import ResourceGroupService

AssignmentTargetRef = str | tuple[str, str] | None


class DatabaseSeeder:
    """Loads `seed_plan` through the regular services, so all business rules apply."""

    def __init__(
        self, uow: SqlAlchemyUnitOfWork, spec_source: ApiSpecSource, settings: Settings
    ) -> None:
        self._uow = uow
        self._spec_source = spec_source
        self._settings = settings

    async def seed(self) -> bool:
        """Return False when the database already holds consumer groups."""
        if await self._uow.consumer_groups.page(0, 1):
            return False
        await DefaultPolicyBootstrapper(self._uow, self._settings).ensure()
        await ResourceDiscoveryService(self._spec_source, self._uow).synchronize()
        policy_ids = await self._seed_policies()
        resource_group_ids = await self._seed_resource_groups()
        consumer_group_ids = await self._seed_consumer_groups()
        await self._seed_assignments(policy_ids, resource_group_ids, consumer_group_ids)
        return True

    async def _seed_policies(self) -> dict[str, int]:
        service = PolicyService(self._uow)
        created = {}
        for name, strategy, requests, period in seed_plan.POLICIES:
            command = PolicyCreate(
                name=name,
                rate_limiting_strategy=RateLimitingStrategy(strategy),
                number_of_requests=requests,
                time_period=period,
            )
            created[name] = (await service.create(command)).id
        return created

    async def _seed_resource_groups(self) -> dict[str, int]:
        resources = await self._uow.resources.page(0, 1000)
        service = ResourceGroupService(self._uow)
        created = {}
        for name, prefix in seed_plan.RESOURCE_GROUPS.items():
            member_ids = [r.id for r in resources if self._has_prefix(r.resource_path, prefix)]
            group = await service.create(ResourceGroupCreate(name=name, resource_ids=member_ids))
            created[name] = group.id
        return created

    async def _seed_consumer_groups(self) -> dict[ConsumerGroupName, int]:
        service = ConsumerGroupService(self._uow)
        created = {}
        for group_name, consumer_ids in seed_plan.CONSUMER_GROUPS.items():
            command = ConsumerGroupCreate(group_name=group_name, consumer_ids=list(consumer_ids))
            created[group_name] = (await service.create(command)).id
        return created

    async def _seed_assignments(
        self,
        policy_ids: dict[str, int],
        resource_group_ids: dict[str, int],
        consumer_group_ids: dict[ConsumerGroupName, int],
    ) -> None:
        service = AssignmentService(self._uow)
        for group_name, relationship_type, target, policy_name in seed_plan.ASSIGNMENTS:
            command = AssignmentCreate(
                consumer_group_id=consumer_group_ids[group_name],
                relationship_type=relationship_type,
                policy_id=policy_ids[policy_name],
                resource_id=await self._resource_id(relationship_type, target),
                resource_group_id=self._group_id(relationship_type, target, resource_group_ids),
            )
            await service.create(command)

    async def _resource_id(
        self, relationship_type: RelationshipType, target: AssignmentTargetRef
    ) -> int | None:
        if relationship_type is not RelationshipType.RESOURCE or not isinstance(target, tuple):
            return None
        method, path = target
        resource = await self._uow.resources.find(HttpMethod(method), path)
        if resource is None:
            raise NotFoundError(f"Seed resource {method} {path} is missing in the target API")
        return resource.id

    @staticmethod
    def _group_id(
        relationship_type: RelationshipType,
        target: AssignmentTargetRef,
        resource_group_ids: dict[str, int],
    ) -> int | None:
        if relationship_type is not RelationshipType.RESOURCE_GROUP or not isinstance(target, str):
            return None
        return resource_group_ids[target]

    @staticmethod
    def _has_prefix(path: str, prefix: str) -> bool:
        return path == prefix or path.startswith(prefix + "/")
