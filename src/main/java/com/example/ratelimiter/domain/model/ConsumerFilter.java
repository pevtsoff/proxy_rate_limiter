package com.example.ratelimiter.domain.model;

/** Optional criteria for listing consumers; {@code null} fields are ignored. */
public record ConsumerFilter(String consumerIdContains, Long consumerGroupId) {

    public static ConsumerFilter none() {
        return new ConsumerFilter(null, null);
    }
}
