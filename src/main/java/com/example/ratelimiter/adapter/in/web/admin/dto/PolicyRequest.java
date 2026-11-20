package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.PolicyCommand;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload of a policy.
 *
 * @param rateLimitingStrategy optional, defaults to {@code moving-window}
 */
public record PolicyRequest(
        @NotBlank @Size(max = 100) String name,
        RateLimitingStrategy rateLimitingStrategy,
        @NotNull @Positive Integer numberOfRequests,
        @NotNull TimePeriod timePeriod) {

    public PolicyCommand toCommand() {
        return new PolicyCommand(name, rateLimitingStrategy, numberOfRequests, timePeriod);
    }
}
