package com.example.ratelimiter.adapter.out.ratelimit;

import com.example.ratelimiter.application.port.out.RateLimiterPort;
import com.example.ratelimiter.domain.exception.DomainException;
import com.example.ratelimiter.domain.exception.RateLimiterUnavailableException;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RateLimiterPort} that dispatches to the {@link RateLimitAlgorithm} of the policy's
 * strategy. All counters live in Redis, which is what makes the limit hold across instances.
 */
public class RedisRateLimiterAdapter implements RateLimiterPort {

    private final Map<RateLimitingStrategy, RateLimitAlgorithm> algorithms;

    public RedisRateLimiterAdapter(List<RateLimitAlgorithm> algorithms) {
        this.algorithms = new EnumMap<>(RateLimitingStrategy.class);
        algorithms.forEach(algorithm -> this.algorithms.put(algorithm.strategy(), algorithm));
        ensureEveryStrategyIsImplemented();
    }

    @Override
    public RateLimitDecision tryConsume(String bucketKey, Policy policy) {
        RateLimitAlgorithm algorithm = algorithms.get(policy.strategy());
        try {
            return algorithm.tryConsume(bucketKey, policy);
        } catch (DomainException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RateLimiterUnavailableException(
                    "Rate limiter store is unavailable: " + exception.getMessage(), exception);
        }
    }

    private void ensureEveryStrategyIsImplemented() {
        for (RateLimitingStrategy strategy : RateLimitingStrategy.values()) {
            if (!algorithms.containsKey(strategy)) {
                throw new IllegalStateException("No algorithm registered for " + strategy.value());
            }
        }
    }
}
