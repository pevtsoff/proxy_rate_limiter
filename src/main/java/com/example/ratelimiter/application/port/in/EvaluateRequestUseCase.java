package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.RequestEvaluation;

/** CUC1.3-4: checks whether a request still fits into the applicable rate limit. */
public interface EvaluateRequestUseCase {

    RequestEvaluation evaluate(String consumerId, HttpMethod method, String path);
}
