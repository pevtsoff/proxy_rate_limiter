from proxy.db.models.resource import Resource
from proxy.domain.value_objects.discovered_endpoint import DiscoveredEndpoint
from proxy.domain.value_objects.sync_report import SyncReport
from proxy.ports.api_spec_source import ApiSpecSource
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork


class ResourceDiscoveryService:
    """Use case MUC1.4: import every endpoint of the target API from its swagger document.

    Synchronisation is additive and idempotent: existing resources keep the io_type and
    group an API manager may have changed.
    """

    def __init__(self, source: ApiSpecSource, uow: SqlAlchemyUnitOfWork) -> None:
        self._source = source
        self._uow = uow

    async def synchronize(self) -> SyncReport:
        endpoints = await self._source.fetch_endpoints()
        known = await self._uow.resources.existing_keys()
        new_endpoints = [e for e in endpoints if (e.method, e.path) not in known]
        for endpoint in new_endpoints:
            self._uow.resources.add(self._to_resource(endpoint))
        await self._uow.commit()
        return SyncReport(discovered=len(endpoints), created=len(new_endpoints))

    @staticmethod
    def _to_resource(endpoint: DiscoveredEndpoint) -> Resource:
        return Resource(
            method=endpoint.method,
            resource_path=endpoint.path,
            io_type=endpoint.method.default_io_type,
        )
