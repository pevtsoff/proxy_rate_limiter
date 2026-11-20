package com.example.ratelimiter.domain.model;

import java.time.Duration;

/**
 * Outcome of consuming one request from a counter.
 *
 * @param allowed    whether the request may be forwarded
 * @param limit      configured number of requests per period
 * @param remaining  requests left in the current period after this one
 * @param retryAfter how long the caller should wait before retrying; zero when allowed
 */
public record RateLimitDecision(boolean allowed, int limit, long remaining, Duration retryAfter) {

    public static RateLimitDecision allowed(int limit, long remaining) {
        return new RateLimitDecision(true, limit, Math.max(0, remaining), Duration.ZERO);
    }

    public static RateLimitDecision rejected(int limit, Duration retryAfter) {
        return new RateLimitDecision(false, limit, 0, retryAfter);
    }

    /** Retry-After in whole seconds, rounded up and never below one second for rejections. */
    public long retryAfterSeconds() {
        if (allowed) {
            return 0;
        }
        long millis = retryAfter.toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }
}
