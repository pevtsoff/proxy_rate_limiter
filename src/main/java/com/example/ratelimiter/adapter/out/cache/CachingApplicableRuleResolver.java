package com.example.ratelimiter.adapter.out.cache;

import com.example.ratelimiter.application.port.in.ResolveApplicableRuleUseCase;
import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.HttpMethod;
import org.springframework.cache.annotation.Cacheable;

/**
 * MUC5: caching decorator around the database-backed rule resolution. The cache key is the
 * default composite key of all arguments, i.e. {@code (consumerId, method, path)}.
 *
 * <p>Keeping caching in a decorator leaves the application service free of framework
 * annotations and lets the cache be switched off by simply not wiring the decorator.
 */
public class CachingApplicableRuleResolver implements ResolveApplicableRuleUseCase {

    private final ResolveApplicableRuleUseCase delegate;

    public CachingApplicableRuleResolver(ResolveApplicableRuleUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Cacheable(cacheNames = RuleCacheNames.APPLICABLE_RULES)
    public ApplicableRule resolve(String consumerId, HttpMethod method, String path) {
        return delegate.resolve(consumerId, method, path);
    }
}
