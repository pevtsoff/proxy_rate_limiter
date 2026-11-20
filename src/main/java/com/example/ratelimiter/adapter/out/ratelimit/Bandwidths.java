package com.example.ratelimiter.adapter.out.ratelimit;

import com.example.ratelimiter.domain.model.Policy;
import io.github.bucket4j.Bandwidth;

/** Bucket4j bandwidth definitions of the supported Bucket4j strategies. */
public final class Bandwidths {

    private Bandwidths() {
    }

    /** {@code token-bucket}: tokens are added continuously, smoothing traffic. */
    public static Bandwidth tokenBucket(Policy policy) {
        return Bandwidth.builder()
                .capacity(policy.numberOfRequests())
                .refillGreedy(policy.numberOfRequests(), policy.timePeriod().duration())
                .build();
    }

    /** {@code fixed-window}: the whole budget is restored at once at the end of each period. */
    public static Bandwidth fixedWindow(Policy policy) {
        return Bandwidth.builder()
                .capacity(policy.numberOfRequests())
                .refillIntervally(policy.numberOfRequests(), policy.timePeriod().duration())
                .build();
    }
}
