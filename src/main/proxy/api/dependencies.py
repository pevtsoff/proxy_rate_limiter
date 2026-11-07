"""FastAPI dependency providers: the only place where services are assembled per request."""

from collections.abc import AsyncIterator
from typing import Annotated

from fastapi import Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from proxy.api.app_container import AppContainer
from proxy.api.pagination import Pagination, get_pagination
from proxy.infrastructure.httpx_upstream_forwarder import HttpxUpstreamForwarder
from proxy.infrastructure.openapi_spec_source import OpenApiSpecSource
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.services.assignment_service import AssignmentService
from proxy.services.consumer_group_service import ConsumerGroupService
from proxy.services.consumer_service import ConsumerService
from proxy.services.policy_resolution.cached_policy_resolver import CachedPolicyResolver
from proxy.services.policy_resolution.database_policy_resolver import DatabasePolicyResolver
from proxy.services.policy_resolution.policy_selector import PolicySelector
from proxy.services.policy_service import PolicyService
from proxy.services.proxying.bearer_authenticator import BearerAuthenticator
from proxy.services.proxying.proxy_request_handler import ProxyRequestHandler
from proxy.services.resource_discovery_service import ResourceDiscoveryService
from proxy.services.resource_group_service import ResourceGroupService
from proxy.services.resource_service import ResourceService
from proxy.services.token_service import TokenService


def get_container(request: Request) -> AppContainer:
    return request.app.state.container


ContainerDep = Annotated[AppContainer, Depends(get_container)]
PaginationDep = Annotated[Pagination, Depends(get_pagination)]


async def get_session(container: ContainerDep) -> AsyncIterator[AsyncSession]:
    async with container.session_factory() as session:
        yield session


SessionDep = Annotated[AsyncSession, Depends(get_session)]


def get_unit_of_work(session: SessionDep, container: ContainerDep) -> SqlAlchemyUnitOfWork:
    return SqlAlchemyUnitOfWork(session, [container.policy_cache])


UnitOfWorkDep = Annotated[SqlAlchemyUnitOfWork, Depends(get_unit_of_work)]


def get_policy_service(uow: UnitOfWorkDep) -> PolicyService:
    return PolicyService(uow)


def get_consumer_group_service(uow: UnitOfWorkDep) -> ConsumerGroupService:
    return ConsumerGroupService(uow)


def get_consumer_service(uow: UnitOfWorkDep) -> ConsumerService:
    return ConsumerService(uow)


def get_resource_service(uow: UnitOfWorkDep) -> ResourceService:
    return ResourceService(uow)


def get_resource_group_service(uow: UnitOfWorkDep) -> ResourceGroupService:
    return ResourceGroupService(uow)


def get_assignment_service(uow: UnitOfWorkDep) -> AssignmentService:
    return AssignmentService(uow)


def get_resource_discovery_service(
    uow: UnitOfWorkDep, container: ContainerDep
) -> ResourceDiscoveryService:
    source = OpenApiSpecSource(container.http_client, container.settings.target_openapi_path)
    return ResourceDiscoveryService(source, uow)


def get_token_service(uow: UnitOfWorkDep, container: ContainerDep) -> TokenService:
    ttl = container.settings.jwt_default_ttl_seconds
    return TokenService(uow, container.token_codec, ttl)


def get_proxy_request_handler(uow: UnitOfWorkDep, container: ContainerDep) -> ProxyRequestHandler:
    database_resolver = DatabasePolicyResolver(uow, PolicySelector.with_default_precedence())
    return ProxyRequestHandler(
        authenticator=BearerAuthenticator(container.token_codec),
        resolver=CachedPolicyResolver(database_resolver, container.policy_cache),
        rate_limiter=container.rate_limiter,
        forwarder=HttpxUpstreamForwarder(container.http_client),
    )


PolicyServiceDep = Annotated[PolicyService, Depends(get_policy_service)]
ConsumerGroupServiceDep = Annotated[ConsumerGroupService, Depends(get_consumer_group_service)]
ConsumerServiceDep = Annotated[ConsumerService, Depends(get_consumer_service)]
ResourceServiceDep = Annotated[ResourceService, Depends(get_resource_service)]
ResourceGroupServiceDep = Annotated[ResourceGroupService, Depends(get_resource_group_service)]
AssignmentServiceDep = Annotated[AssignmentService, Depends(get_assignment_service)]
ResourceDiscoveryServiceDep = Annotated[
    ResourceDiscoveryService, Depends(get_resource_discovery_service)
]
TokenServiceDep = Annotated[TokenService, Depends(get_token_service)]
ProxyRequestHandlerDep = Annotated[ProxyRequestHandler, Depends(get_proxy_request_handler)]
