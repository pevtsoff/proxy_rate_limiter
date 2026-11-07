from proxy.domain.errors.proxy_error import ProxyError


class ConflictError(ProxyError):
    """The requested change contradicts existing data or a business rule."""

    status_code = 409
