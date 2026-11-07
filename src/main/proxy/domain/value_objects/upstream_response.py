from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class UpstreamResponse:
    status_code: int
    headers: tuple[tuple[str, str], ...]
    body: bytes
