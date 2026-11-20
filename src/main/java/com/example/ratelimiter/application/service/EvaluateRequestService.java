package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.EvaluateRequestUseCase;
import com.example.ratelimiter.application.port.in.ResolveApplicableRuleUseCase;
import com.example.ratelimiter.application.port.out.RateLimiterPort;
import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RequestEvaluation;

/** Resolves the applicable rule (usually from cache) and consumes from its shared counter. */
public final class EvaluateRequestService implements EvaluateRequestUseCase {

    private final ResolveApplicableRuleUseCase ruleResolver;
    private final RateLimiterPort rateLimiter;

    public EvaluateRequestService(ResolveApplicableRuleUseCase ruleResolver,
            RateLimiterPort rateLimiter) {
        this.ruleResolver = ruleResolver;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public RequestEvaluation evaluate(String consumerId, HttpMethod method, String path) {
        ApplicableRule rule = ruleResolver.resolve(consumerId, method, path);
        RateLimitDecision decision = rateLimiter.tryConsume(rule.bucketKey(consumerId),
                rule.policy());
        return new RequestEvaluation(rule, decision);
    }
}
