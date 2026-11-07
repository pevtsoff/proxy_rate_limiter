from contextlib import suppress

from proxy.config.settings import Settings
from proxy.db.models.policy import Policy
from proxy.domain.errors.conflict_error import ConflictError
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork


class DefaultPolicyBootstrapper:
    """Pre-creates the default policy from settings (MUC3). Safe to run concurrently."""

    def __init__(self, uow: SqlAlchemyUnitOfWork, settings: Settings) -> None:
        self._uow = uow
        self._settings = settings

    async def ensure(self) -> Policy:
        existing = await self._uow.policies.get_default()
        if existing is not None:
            return existing
        self._uow.policies.add(self._build_default())
        # Another proxy worker may create it first; then read theirs below.
        with suppress(ConflictError):
            await self._uow.commit()
        return await self._read_default()

    async def _read_default(self) -> Policy:
        policy = await self._uow.policies.get_default()
        if policy is None:
            raise LookupError(
                f"Could not create the default policy; is the name "
                f"'{self._settings.default_policy_name}' taken by another policy?"
            )
        return policy

    def _build_default(self) -> Policy:
        return Policy(
            name=self._settings.default_policy_name,
            rate_limiting_strategy=self._settings.default_policy_strategy,
            number_of_requests=self._settings.default_policy_requests,
            time_period=self._settings.default_policy_period,
            is_default=True,
        )
