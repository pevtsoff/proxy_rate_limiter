from proxy.domain.errors.proxy_error import ProxyError


class NotFoundError(ProxyError):
    status_code = 404
