from proxy.domain.errors.proxy_error import ProxyError


class UpstreamUnavailableError(ProxyError):
    status_code = 502
