from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class LruCacheConfig:
    capacity: int
    ttl_seconds: int
