package com.example.ratelimiter.adapter.out.ratelimit;

import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;

/**
 * Bucket4j-backed strategy. The bucket state lives in Redis and is updated with Bucket4j's
 * compare-and-swap protocol, so concurrent requests on any number of instances are counted
 * exactly once. The strategy-specific part is only the {@link BandwidthFactory} (composition
 * instead of a subclass per strategy).
 */
public class Bucket4jAlgorithm implements RateLimitAlgorithm {

    private final RateLimitingStrategy strategy;
    private final ProxyManager<String> proxyManager;
    private final BandwidthFactory bandwidthFactory;

    public Bucket4jAlgorithm(
            RateLimitingStrategy strategy,
            ProxyManager<String> proxyManager,
            BandwidthFactory bandwidthFactory) {
        this.strategy = strategy;
        this.proxyManager = proxyManager;
        this.bandwidthFactory = bandwidthFactory;
    }

    @Override
    public RateLimitingStrategy strategy() {
        return strategy;
    }

    @Override
    public RateLimitDecision tryConsume(String key, Policy policy) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(bandwidthFactory.create(policy))
                .build();
        ConsumptionProbe probe = proxyManager.builder()
                .build(key, () -> configuration)
                .tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.allowed(policy.numberOfRequests(), probe.getRemainingTokens());
        }
        return RateLimitDecision.rejected(
                policy.numberOfRequests(), Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }
}
