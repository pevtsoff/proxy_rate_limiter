from proxy.domain.errors.proxy_error import ProxyError


class UnknownConsumerError(ProxyError):
    """The token is valid but its consumer is not registered."""

    status_code = 403
