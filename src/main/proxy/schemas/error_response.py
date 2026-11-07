from pydantic import BaseModel


class ErrorResponse(BaseModel):
    """The single JSON shape of every user facing error."""

    error: str
    resource: str
    https_status_code: int
