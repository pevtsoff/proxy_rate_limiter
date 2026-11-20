package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.InvalidateRuleCacheUseCase;
import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;

/** Manual rule-cache invalidation. */
public final class RuleCacheService implements InvalidateRuleCacheUseCase {

    private final RuleCacheInvalidationPort ruleCache;

    public RuleCacheService(RuleCacheInvalidationPort ruleCache) {
        this.ruleCache = ruleCache;
    }

    @Override
    public void invalidateAll() {
        ruleCache.invalidateAll();
    }
}
