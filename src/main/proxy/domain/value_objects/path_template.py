import re
from dataclasses import dataclass
from functools import lru_cache

_PARAMETER = re.compile(r"\{[^}/]+\}")
_SEGMENT = "[^/]+"


@lru_cache(maxsize=1024)
def _compile(template: str) -> re.Pattern[str]:
    literal_parts = _PARAMETER.split(template)
    return re.compile(_SEGMENT.join(re.escape(part) for part in literal_parts))


def normalize_path(path: str) -> str:
    """Return `path` with exactly one leading slash and no trailing slash."""
    return "/" + path.strip("/")


@dataclass(frozen=True, slots=True)
class PathTemplate:
    """An OpenAPI path such as `/orders/{item_id}` that concrete paths are matched to."""

    template: str

    @property
    def parameter_count(self) -> int:
        return len(_PARAMETER.findall(self.template))

    def matches(self, path: str) -> bool:
        pattern = _compile(normalize_path(self.template))
        return pattern.fullmatch(normalize_path(path)) is not None
