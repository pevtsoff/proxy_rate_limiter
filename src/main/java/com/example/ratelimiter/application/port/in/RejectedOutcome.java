package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.RequestEvaluation;

/** The consumer exhausted its capacity; the request was not forwarded. */
public record RejectedOutcome(RequestEvaluation evaluation) implements ProxyOutcome {
}
