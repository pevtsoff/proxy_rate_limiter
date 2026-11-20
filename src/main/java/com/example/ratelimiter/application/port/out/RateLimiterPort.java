package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;

/**
 * Distributed counter store. Implementations must be atomic across application instances.
 */
public interface RateLimiterPort {

    /**
     * Atomically consumes one request from the counter identified by {@code bucketKey}.
     *
     * @throws com.example.ratelimiter.domain.exception.RateLimiterUnavailableException when the
     *         store cannot be reached
     */
    RateLimitDecision tryConsume(String bucketKey, Policy policy);
}
