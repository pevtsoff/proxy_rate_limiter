package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.RequestEvaluation;

/** Result of handling a proxied request: either forwarded or rejected by the rate limiter. */
public sealed interface ProxyOutcome permits ForwardedOutcome, RejectedOutcome {

    RequestEvaluation evaluation();
}
