package com.example.ratelimiter.application.port.out;

/**
 * Invalidates cached {@code (consumer, path) -> rule} resolutions after configuration changes,
 * on this instance and on every other instance.
 */
public interface RuleCacheInvalidationPort {

    void invalidateAll();
}
