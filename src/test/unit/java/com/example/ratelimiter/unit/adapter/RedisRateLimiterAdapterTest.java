package com.example.ratelimiter.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ratelimiter.adapter.out.ratelimit.RateLimitAlgorithm;
import com.example.ratelimiter.adapter.out.ratelimit.RedisRateLimiterAdapter;
import com.example.ratelimiter.domain.exception.RateLimiterUnavailableException;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RedisRateLimiterAdapterTest {

    @Test
    void dispatchesToTheAlgorithmOfThePolicyStrategy() {
        List<String> calls = new ArrayList<>();
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(List.of(
                recording(RateLimitingStrategy.MOVING_WINDOW, calls),
                recording(RateLimitingStrategy.FIXED_WINDOW, calls),
                recording(RateLimitingStrategy.TOKEN_BUCKET, calls)));

        adapter.tryConsume("k", policy(RateLimitingStrategy.FIXED_WINDOW));

        assertThat(calls).containsExactly("fixed-window:k");
    }

    @Test
    void refusesToStartWhenAStrategyHasNoAlgorithm() {
        assertThatThrownBy(() -> new RedisRateLimiterAdapter(
                List.of(recording(RateLimitingStrategy.MOVING_WINDOW, new ArrayList<>()))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void storeFailuresBecomeRateLimiterUnavailable() {
        RateLimitAlgorithm failing = new RateLimitAlgorithm() {
            @Override
            public RateLimitingStrategy strategy() {
                return RateLimitingStrategy.MOVING_WINDOW;
            }

            @Override
            public RateLimitDecision tryConsume(String key, Policy policy) {
                throw new IllegalStateException("connection refused");
            }
        };
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(List.of(failing,
                recording(RateLimitingStrategy.FIXED_WINDOW, new ArrayList<>()),
                recording(RateLimitingStrategy.TOKEN_BUCKET, new ArrayList<>())));

        assertThatThrownBy(() -> adapter.tryConsume("k",
                policy(RateLimitingStrategy.MOVING_WINDOW)))
                .isInstanceOf(RateLimiterUnavailableException.class);
    }

    private static RateLimitAlgorithm recording(RateLimitingStrategy strategy, List<String> calls) {
        return new RateLimitAlgorithm() {
            @Override
            public RateLimitingStrategy strategy() {
                return strategy;
            }

            @Override
            public RateLimitDecision tryConsume(String key, Policy policy) {
                calls.add(strategy.value() + ":" + key);
                return RateLimitDecision.allowed(policy.numberOfRequests(), 0);
            }
        };
    }

    private static Policy policy(RateLimitingStrategy strategy) {
        return new Policy(1L, "p", strategy, 1, TimePeriod.SECOND, false);
    }
}
