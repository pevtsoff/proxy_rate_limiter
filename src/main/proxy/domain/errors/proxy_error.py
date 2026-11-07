class ProxyError(Exception):
    """Base class of every error that is reported to callers as JSON.

    `status_code` is the HTTP status the error maps to; `resource` is the path the
    caller tried to reach (filled from the request when omitted).
    """

    status_code: int = 500

    def __init__(self, message: str, resource: str | None = None) -> None:
        super().__init__(message)
        self.message = message
        self.resource = resource
