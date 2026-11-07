from dataclasses import dataclass
from typing import Annotated

from fastapi import Query

MAX_PAGE_SIZE = 500


@dataclass(frozen=True, slots=True)
class Pagination:
    offset: int
    limit: int


def get_pagination(
    offset: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=MAX_PAGE_SIZE)] = 100,
) -> Pagination:
    return Pagination(offset, limit)
