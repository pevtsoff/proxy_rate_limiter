from proxy.domain.errors.proxy_error import ProxyError


class DomainValidationError(ProxyError, ValueError):
    """Invalid domain value. Inherits ValueError so pydantic reports it as a 422."""

    status_code = 422
