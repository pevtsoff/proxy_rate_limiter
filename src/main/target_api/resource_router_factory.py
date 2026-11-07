"""Builds the static CRUD-ish API of one resource.

GET and PUT are idempotent (same input, same output); POST creates a new id and PATCH
bumps a revision counter on every call, so they are not.
"""

import itertools

from fastapi import APIRouter, status

from target_api.item_payload import ItemPayload
from target_api.item_read import ItemRead

FIRST_CREATED_ID = 1000
STATIC_ITEM_IDS = (1, 2, 3)


def build_resource_router(resource: str) -> APIRouter:
    router = APIRouter(prefix=f"/{resource}", tags=[resource])
    _register_reads(router, resource)
    _register_writes(router, resource)
    return router


def _register_reads(router: APIRouter, resource: str) -> None:
    @router.get("", operation_id=f"list_{resource}")
    async def list_items() -> list[ItemRead]:
        return [ItemRead(id=item_id, resource=resource) for item_id in STATIC_ITEM_IDS]

    @router.get("/{item_id}", operation_id=f"get_{resource}")
    async def get_item(item_id: int) -> ItemRead:
        return ItemRead(id=item_id, resource=resource)


def _register_writes(router: APIRouter, resource: str) -> None:
    new_ids = itertools.count(FIRST_CREATED_ID)
    revisions = itertools.count(1)

    @router.post("", status_code=status.HTTP_201_CREATED, operation_id=f"create_{resource}")
    async def create_item(payload: ItemPayload) -> ItemRead:
        return ItemRead(id=next(new_ids), resource=resource, data=payload.model_dump())

    @router.put("/{item_id}", operation_id=f"replace_{resource}")
    async def replace_item(item_id: int, payload: ItemPayload) -> ItemRead:
        return ItemRead(id=item_id, resource=resource, data=payload.model_dump())

    @router.patch("/{item_id}", operation_id=f"patch_{resource}")
    async def patch_item(item_id: int, payload: ItemPayload) -> ItemRead:
        data = payload.model_dump(exclude_unset=True)
        return ItemRead(id=item_id, resource=resource, data=data, revision=next(revisions))
