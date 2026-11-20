package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.HttpMethod;

/**
 * Determines which policy applies to a consumer calling a REST path (MUC5: the result of this
 * use case is what the LRU cache stores).
 */
public interface ResolveApplicableRuleUseCase {

    ApplicableRule resolve(String consumerId, HttpMethod method, String path);
}
