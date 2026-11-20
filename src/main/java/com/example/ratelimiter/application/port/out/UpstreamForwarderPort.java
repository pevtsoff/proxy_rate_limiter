package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.ProxiedRequest;
import com.example.ratelimiter.domain.model.UpstreamResponse;

/** Sends an allowed request to the target API. */
public interface UpstreamForwarderPort {

    UpstreamResponse forward(ProxiedRequest request);
}
