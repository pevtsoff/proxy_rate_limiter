package com.example.ratelimiter.domain.model;

/** Optional criteria for listing policies; {@code null} fields are ignored. */
public record PolicyFilter(String nameContains, RateLimitingStrategy strategy) {

    public static PolicyFilter none() {
        return new PolicyFilter(null, null);
    }
}
