"""`python -m proxy.seeding`: load demo data and print bearer tokens for demo consumers."""

import asyncio
import json
import logging
from collections.abc import Callable

from proxy.api.container_factory import build_container
from proxy.config.logging_config import configure_logging
from proxy.config.settings import get_settings
from proxy.infrastructure.openapi_spec_source import OpenApiSpecSource
from proxy.repositories.sqlalchemy_unit_of_work import SqlAlchemyUnitOfWork
from proxy.seeding import seed_plan
from proxy.seeding.database_seeder import DatabaseSeeder

logger = logging.getLogger(__name__)


async def run() -> None:
    settings = get_settings()
    configure_logging(settings.log_level)
    container = build_container(settings)
    try:
        async with container.session_factory() as session:
            uow = SqlAlchemyUnitOfWork(session, [container.policy_cache])
            source = OpenApiSpecSource(container.http_client, settings.target_openapi_path)
            seeded = await DatabaseSeeder(uow, source, settings).seed()
        logger.info("Seeding %s", "completed" if seeded else "skipped: data already present")
        _print_demo_tokens(container.token_codec.encode, settings.jwt_default_ttl_seconds)
    finally:
        await container.aclose()


def _print_demo_tokens(encode: Callable[[str, int], str], ttl_seconds: int) -> None:
    tokens = {
        consumer_id: encode(consumer_id, ttl_seconds)
        for consumer_ids in seed_plan.CONSUMER_GROUPS.values()
        for consumer_id in consumer_ids
    }
    print(json.dumps(tokens, indent=2))


if __name__ == "__main__":
    asyncio.run(run())
