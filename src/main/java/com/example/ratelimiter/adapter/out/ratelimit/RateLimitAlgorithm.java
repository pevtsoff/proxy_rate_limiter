package com.example.ratelimiter.adapter.out.ratelimit;

import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;

/**
 * One rate-limiting algorithm backed by Redis. New strategies are added by implementing this
 * interface and registering a bean; nothing else changes (open/closed principle).
 */
public interface RateLimitAlgorithm {

    RateLimitingStrategy strategy();

    /** Atomically consumes one request from the counter {@code key}. */
    RateLimitDecision tryConsume(String key, Policy policy);
}
