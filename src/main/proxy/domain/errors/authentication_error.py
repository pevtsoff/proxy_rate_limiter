from proxy.domain.errors.proxy_error import ProxyError


class AuthenticationError(ProxyError):
    status_code = 401
