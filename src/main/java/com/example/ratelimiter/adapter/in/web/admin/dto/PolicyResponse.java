package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;

/** Policy representation returned by the admin API. */
public record PolicyResponse(
        Long id,
        String name,
        RateLimitingStrategy rateLimitingStrategy,
        int numberOfRequests,
        TimePeriod timePeriod,
        boolean defaultPolicy) {

    public static PolicyResponse from(Policy policy) {
        return new PolicyResponse(policy.id(), policy.name(), policy.strategy(),
                policy.numberOfRequests(), policy.timePeriod(), policy.defaultPolicy());
    }
}
