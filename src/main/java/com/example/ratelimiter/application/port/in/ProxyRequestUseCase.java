package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.ProxiedRequest;

/** CUC1: evaluate a consumer request and forward it to the target API when allowed. */
public interface ProxyRequestUseCase {

    ProxyOutcome handle(ProxiedRequest request);
}
