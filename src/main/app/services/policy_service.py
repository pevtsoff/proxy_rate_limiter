"""Resolve and enforce distributed policies."""

import json
import math
import time
from collections import OrderedDict

from app.config import settings
from app.database import redis_client
from app.models import Consumer, ConsumerGroupPolicy, Policy, Resource
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.routing import compile_path

_cache: OrderedDict[str, tuple[float, dict[str, int | str]]] = OrderedDict()
PERIOD_SECONDS = {"second": 1, "minute": 60, "hour": 3600, "day": 86400, "week": 604800}


async def resolve_policy(
    session: AsyncSession, consumer_id: str, path: str, method: str
) -> dict[str, int | str]:
    """Resolve exact-resource then resource-group policy, with a bounded local cache."""
    key = f"{consumer_id}:{method}:{path}"
    cached = await get_cached_policy(key)
    if cached is not None:
        return cached
    policy = await load_applicable_policy(session, consumer_id, path, method)
    result = policy_values(policy)
    await cache_policy(key, result)
    return result


async def get_cached_policy(key: str) -> dict[str, int | str] | None:
    now = time.monotonic()
    entry = _cache.get(key)
    if entry and entry[0] > now:
        _cache.move_to_end(key)
        return entry[1]
    cached_json = await redis_client.get(f"policy-cache:{key}")
    if not cached_json:
        return None
    cached_policy = json.loads(cached_json)
    remember_policy(key, now, cached_policy)
    return cached_policy


async def load_applicable_policy(
    session: AsyncSession, consumer_id: str, path: str, method: str
) -> Policy | None:
    group_id = await session.scalar(
        select(Consumer.consumer_group_id).where(Consumer.id == consumer_id)
    )
    if group_id is None:
        return None
    resource = await find_resource(session, method, path)
    if resource is None:
        return None
    assignment = await find_resource_assignment(session, group_id, resource.id)
    if assignment is None:
        assignment = await find_group_assignment(session, group_id, resource.resource_group_id)
    return await session.get(Policy, assignment.policy_id) if assignment else None


async def find_resource_assignment(
    session: AsyncSession, group_id: int, resource_id: int
) -> ConsumerGroupPolicy | None:
    return await session.scalar(
        select(ConsumerGroupPolicy).where(
            ConsumerGroupPolicy.consumer_group_id == group_id,
            ConsumerGroupPolicy.relationship_type == "resource",
            ConsumerGroupPolicy.resource_id == resource_id,
        )
    )


async def find_group_assignment(
    session: AsyncSession, group_id: int, resource_group_id: int
) -> ConsumerGroupPolicy | None:
    return await session.scalar(
        select(ConsumerGroupPolicy).where(
            ConsumerGroupPolicy.consumer_group_id == group_id,
            ConsumerGroupPolicy.relationship_type == "resource_group",
            ConsumerGroupPolicy.resource_id == resource_group_id,
        )
    )


def policy_values(policy: Policy | None) -> dict[str, int | str]:
    if policy is None:
        return {
            "limit": settings.default_requests,
            "period": PERIOD_SECONDS.get(settings.default_period, 60),
            "strategy": "moving-window",
        }
    return {
        "limit": policy.number_of_requests,
        "period": PERIOD_SECONDS.get(policy.time_period, 60),
        "strategy": policy.rate_limiting_strategy,
    }


async def cache_policy(key: str, policy: dict[str, int | str]) -> None:
    remember_policy(key, time.monotonic(), policy)
    await redis_client.set(f"policy-cache:{key}", json.dumps(policy), ex=settings.lru_cache_ttl)


def remember_policy(key: str, now: float, policy: dict[str, int | str]) -> None:
    """Store a policy in the bounded local LRU cache."""
    _cache[key] = (now + settings.lru_cache_ttl, policy)
    _cache.move_to_end(key)
    while len(_cache) > settings.lru_cache_capacity:
        _cache.popitem(last=False)


async def find_resource(session: AsyncSession, method: str, path: str) -> Resource | None:
    """Find the OpenAPI path template matching a concrete request path."""
    resources = (await session.scalars(select(Resource).where(Resource.method == method))).all()
    for resource in resources:
        regex, _, _ = compile_path(resource.resource_path)
        if regex.fullmatch(path):
            return resource
    return None


async def consume_capacity(key: str, limit: int, period: int) -> tuple[bool, int]:
    """Atomically increment a Redis fixed-window counter across proxy instances."""
    window = math.floor(time.time() / period)
    redis_key = f"rate:{key}:{window}"
    count = await redis_client.incr(redis_key)
    if count == 1:
        await redis_client.expire(redis_key, period + 1)
    return count <= limit, max(0, limit - count)


async def invalidate_policy_cache() -> None:
    """Invalidate policy lookups after admin mutations."""
    _cache.clear()
    keys = [key async for key in redis_client.scan_iter("policy-cache:*")]
    if keys:
        await redis_client.delete(*keys)


def clear_local_policy_cache() -> None:
    """Clear process-local policy entries, for cache lifecycle management."""
    _cache.clear()
