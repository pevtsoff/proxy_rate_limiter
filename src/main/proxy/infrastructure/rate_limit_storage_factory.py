from limits.aio.storage import Storage
from limits.storage import storage_from_string


def create_rate_limit_storage(storage_url: str) -> Storage:
    """Build the async `limits` storage; Redis uses redis-py's asyncio client."""
    if "redis" in storage_url.split("://", 1)[0]:
        storage = storage_from_string(storage_url, implementation="redispy")
    else:
        storage = storage_from_string(storage_url)
    if not isinstance(storage, Storage):
        raise ValueError(f"'{storage_url}' is not an async storage; use an 'async+' scheme")
    return storage
