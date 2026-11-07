from proxy.db.models.resource import Resource
from proxy.domain.value_objects.request_target import RequestTarget
from proxy.repositories.resource_repository import ResourceRepository


class ResourceLocator:
    """Maps a concrete request (`GET /orders/1`) to its resource (`GET /orders/{item_id}`).

    When several templates match, the most specific one (fewest parameters) wins, so
    `/orders/summary` beats `/orders/{item_id}`.
    """

    def __init__(self, resources: ResourceRepository) -> None:
        self._resources = resources

    async def locate(self, target: RequestTarget) -> Resource | None:
        candidates = await self._resources.list_by_method(target.method)
        matching = [r for r in candidates if r.path_template.matches(target.path)]
        return min(matching, key=lambda r: r.path_template.parameter_count, default=None)
