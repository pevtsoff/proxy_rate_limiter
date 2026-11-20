package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.RequestEvaluation;
import com.example.ratelimiter.domain.model.UpstreamResponse;

/** The request was within its limit and the target API answered. */
public record ForwardedOutcome(RequestEvaluation evaluation, UpstreamResponse response)
        implements ProxyOutcome {
}
