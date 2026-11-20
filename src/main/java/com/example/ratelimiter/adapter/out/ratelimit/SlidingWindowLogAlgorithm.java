package com.example.ratelimiter.adapter.out.ratelimit;

import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * {@code moving-window}: exact sliding-window log stored in a Redis sorted set.
 *
 * <p>Bucket4j has no exact sliding window, so this strategy is a Lua script: trimming expired
 * entries, counting, and adding the new entry happen atomically inside Redis, and the script
 * uses Redis' own clock so application instances with skewed clocks still agree.
 */
public class SlidingWindowLogAlgorithm implements RateLimitAlgorithm {

    private static final String RESULT_SEPARATOR = ":";

    private final StringRedisTemplate redis;
    private final RedisScript<String> script;

    public SlidingWindowLogAlgorithm(StringRedisTemplate redis, RedisScript<String> script) {
        this.redis = redis;
        this.script = script;
    }

    @Override
    public RateLimitingStrategy strategy() {
        return RateLimitingStrategy.MOVING_WINDOW;
    }

    @Override
    public RateLimitDecision tryConsume(String key, Policy policy) {
        String result = redis.execute(script, List.of(key),
                String.valueOf(policy.numberOfRequests()),
                String.valueOf(policy.timePeriod().duration().toMillis()),
                UUID.randomUUID().toString());
        return toDecision(result, policy.numberOfRequests());
    }

    private static RateLimitDecision toDecision(String result, int limit) {
        String[] parts = result.split(RESULT_SEPARATOR);
        boolean allowed = "1".equals(parts[0]);
        if (allowed) {
            return RateLimitDecision.allowed(limit, Long.parseLong(parts[1]));
        }
        return RateLimitDecision.rejected(limit, Duration.ofMillis(Long.parseLong(parts[2])));
    }
}
