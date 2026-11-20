package com.example.ratelimiter.adapter.out.cache;

/** Names of the Spring caches used by the proxy. */
public final class RuleCacheNames {

    /** {@code (consumer_id, method, rest_path) -> ApplicableRule}. */
    public static final String APPLICABLE_RULES = "applicable-rules";

    private RuleCacheNames() {
    }
}
