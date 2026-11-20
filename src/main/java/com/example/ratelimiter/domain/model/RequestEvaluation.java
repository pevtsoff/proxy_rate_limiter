package com.example.ratelimiter.domain.model;

/** The rule that was applied to a request together with the resulting decision. */
public record RequestEvaluation(ApplicableRule rule, RateLimitDecision decision) {

    public boolean allowed() {
        return decision.allowed();
    }
}
