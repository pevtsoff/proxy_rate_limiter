"""Resolve and enforce distributed policies."""

import json
import math
import time
from collections import OrderedDict

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.routing import compile_path

from app.config import settings
from app.database import redis_client
from app.models.entities import Consumer, ConsumerGroupPolicy, Policy, Resource, ResourceGroup

_cache: OrderedDict[str, tuple[float, dict[str, int | str]]] = OrderedDict()
PERIOD_SECONDS = {"second": 1, "minute": 60, "hour": 3600, "day": 86400, "week": 604800}


async def resolve_policy(session: AsyncSession, consumer_id: str, path: str, method: str) -> dict[str, int | str]:
    """Resolve exact-resource then resource-group policy, with a bounded local cache."""
    key = f"{consumer_id}:{method}:{path}"
    now = time.monotonic()
    if key in _cache and _cache[key][0] > now:
        _cache.move_to_end(key)
        return _cache[key][1]
    cached_json = await redis_client.get(f"policy-cache:{key}")
    if cached_json:
        cached_policy = json.loads(cached_json)
        remember_policy(key, now, cached_policy)
        return cached_policy
    group_id = await session.scalar(select(Consumer.consumer_group_id).where(Consumer.id == consumer_id))
    assignment = None
    if group_id is not None:
        resource = await find_resource(session, method, path)
        if resource is not None:
            assignment = await session.scalar(select(ConsumerGroupPolicy).where(
                ConsumerGroupPolicy.consumer_group_id == group_id,
                ConsumerGroupPolicy.relationship_type == "resource",
                ConsumerGroupPolicy.resource_id == resource.id,
            ))
            if assignment is None:
                group = await session.get(ResourceGroup, resource.resource_group_id)
                if group is not None:
                    assignment = await session.scalar(select(ConsumerGroupPolicy).where(
                        ConsumerGroupPolicy.consumer_group_id == group_id,
                        ConsumerGroupPolicy.relationship_type == "resource_group",
                        ConsumerGroupPolicy.resource_id == group.id,
                    ))
    policy = await session.get(Policy, assignment.policy_id) if assignment else None
    result: dict[str, int | str] = {
        "limit": policy.number_of_requests if policy else settings.default_requests,
        "period": PERIOD_SECONDS.get(policy.time_period, 60) if policy else PERIOD_SECONDS.get(settings.default_period, 60),
        "strategy": policy.rate_limiting_strategy if policy else "moving-window",
    }
    remember_policy(key, now, result)
    await redis_client.set(
        f"policy-cache:{key}", json.dumps(result), ex=settings.lru_cache_ttl
    )
    return result


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
