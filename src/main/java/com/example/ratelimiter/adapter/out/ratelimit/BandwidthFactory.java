package com.example.ratelimiter.adapter.out.ratelimit;

import com.example.ratelimiter.domain.model.Policy;
import io.github.bucket4j.Bandwidth;

/** Translates a policy into the Bucket4j bandwidth that implements its strategy. */
@FunctionalInterface
public interface BandwidthFactory {

    Bandwidth create(Policy policy);
}
