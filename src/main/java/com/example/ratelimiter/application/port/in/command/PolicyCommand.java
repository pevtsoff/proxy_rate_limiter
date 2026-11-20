package com.example.ratelimiter.application.port.in.command;

import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;

/** Data needed to create or update a rate-limiting policy. */
public record PolicyCommand(
        String name, RateLimitingStrategy strategy, int numberOfRequests, TimePeriod timePeriod) {
}
