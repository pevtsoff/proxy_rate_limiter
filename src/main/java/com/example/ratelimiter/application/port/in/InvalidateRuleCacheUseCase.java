package com.example.ratelimiter.application.port.in;

/**
 * Drops every cached rule resolution on all instances. Needed after data was changed outside
 * the application (e.g. by {@code seed.sh}), which the automatic eviction cannot observe.
 */
public interface InvalidateRuleCacheUseCase {

    void invalidateAll();
}
