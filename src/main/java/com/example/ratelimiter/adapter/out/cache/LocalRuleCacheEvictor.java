package com.example.ratelimiter.adapter.out.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;

/**
 * Evicts this instance's rule cache using Spring's {@link CacheEvict}. Every entry is evicted
 * because a single policy or assignment change can affect any number of
 * {@code (consumer, path)} keys and configuration changes are rare compared to lookups.
 */
public class LocalRuleCacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(LocalRuleCacheEvictor.class);

    @CacheEvict(cacheNames = RuleCacheNames.APPLICABLE_RULES, allEntries = true)
    public void evictAll() {
        LOG.info("Evicted local cache '{}'", RuleCacheNames.APPLICABLE_RULES);
    }
}
