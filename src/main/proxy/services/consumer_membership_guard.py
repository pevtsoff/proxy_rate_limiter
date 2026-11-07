from proxy.db.models.consumer import Consumer
from proxy.domain.errors.conflict_error import ConflictError
from proxy.repositories.consumer_repository import ConsumerRepository


class ConsumerMembershipGuard:
    """Protects the invariant "a consumer group always has at least one consumer"."""

    def __init__(self, consumers: ConsumerRepository) -> None:
        self._consumers = consumers

    async def ensure_can_leave_group(self, consumer: Consumer) -> None:
        if await self._consumers.count_in_group(consumer.consumer_group_id) <= 1:
            raise ConflictError(
                f"Consumer '{consumer.id}' is the last consumer of its group; "
                "a consumer group must keep at least one consumer"
            )
