package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/**
 * A named rate-limiting rule: at most {@code numberOfRequests} per {@code timePeriod}, enforced
 * with {@code strategy}.
 *
 * @param id               database identity, {@code null} for a policy not yet persisted
 * @param defaultPolicy    {@code true} for the single fallback policy used when nothing is assigned
 */
public record Policy(
        Long id,
        String name,
        RateLimitingStrategy strategy,
        int numberOfRequests,
        TimePeriod timePeriod,
        boolean defaultPolicy) {

    public Policy {
        requireText(name);
        if (numberOfRequests <= 0) {
            throw new ValidationException("number_of_requests must be a positive integer");
        }
        if (timePeriod == null) {
            throw new ValidationException("time_period is required");
        }
        strategy = strategy == null ? RateLimitingStrategy.DEFAULT : strategy;
    }

    /** Creates a new, not yet persisted, non-default policy. */
    public static Policy newPolicy(
            String name, RateLimitingStrategy strategy, int numberOfRequests, TimePeriod period) {
        return new Policy(null, name, strategy, numberOfRequests, period, false);
    }

    /** Returns a copy with the editable attributes replaced; identity and default flag stay. */
    public Policy withDefinition(
            String newName, RateLimitingStrategy newStrategy, int newRequests,
                    TimePeriod newPeriod) {
        return new Policy(id, newName, newStrategy, newRequests, newPeriod, defaultPolicy);
    }

    /**
     * Identifies the exact limit definition. It is part of every counter key so that editing a
     * policy starts fresh counters instead of reinterpreting counters of the old definition.
     */
    public String fingerprint() {
        return id + "-" + strategy.value() + "-" + numberOfRequests + "-" + timePeriod.value();
    }

    private static void requireText(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Policy name is required");
        }
    }
}
