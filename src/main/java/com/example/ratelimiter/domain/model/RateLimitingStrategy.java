package com.example.ratelimiter.domain.model;

/**
 * Rate-limiting algorithms supported by the proxy.
 *
 * <p>{@link #MOVING_WINDOW} is the default: it is the only strategy that never lets a burst of
 * up to {@code 2 * limit} requests through around a window boundary.
 */
public enum RateLimitingStrategy implements CodedEnum {

    /** Exact sliding-window log: at most N requests in any rolling period. */
    MOVING_WINDOW("moving-window"),

    /** Fixed window: the budget is fully restored once per period (Bucket4j interval refill). */
    FIXED_WINDOW("fixed-window"),

    /** Token bucket: tokens are refilled continuously (Bucket4j greedy refill). */
    TOKEN_BUCKET("token-bucket");

    /** Strategy used when a policy does not specify one. */
    public static final RateLimitingStrategy DEFAULT = MOVING_WINDOW;

    private final String value;

    RateLimitingStrategy(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static RateLimitingStrategy fromValue(String value) {
        return CodedEnums.fromValue(RateLimitingStrategy.class, value);
    }
}
