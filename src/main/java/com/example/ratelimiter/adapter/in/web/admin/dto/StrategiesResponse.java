package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import java.util.List;

/** Supported values for policy definitions. */
public record StrategiesResponse(
        List<RateLimitingStrategy> supportedStrategies,
        RateLimitingStrategy defaultStrategy,
        List<TimePeriod> supportedTimePeriods) {
}
