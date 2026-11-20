package com.example.ratelimiter.adapter.in.web.proxy;

import com.example.ratelimiter.domain.model.RequestEvaluation;
import org.springframework.http.HttpHeaders;

/** Response headers that tell callers about their remaining budget. */
public final class RateLimitHeaders {

    public static final String LIMIT = "X-RateLimit-Limit";
    public static final String REMAINING = "X-RateLimit-Remaining";
    public static final String POLICY = "X-RateLimit-Policy";
    public static final String STRATEGY = "X-RateLimit-Strategy";

    private RateLimitHeaders() {
    }

    public static void apply(RequestEvaluation evaluation, HttpHeaders headers) {
        headers.set(LIMIT, String.valueOf(evaluation.decision().limit()));
        headers.set(REMAINING, String.valueOf(evaluation.decision().remaining()));
        headers.set(POLICY, evaluation.rule().policy().name() + ";w="
                + evaluation.rule().policy().timePeriod().duration().toSeconds());
        headers.set(STRATEGY, evaluation.rule().policy().strategy().value());
        if (!evaluation.allowed()) {
            headers.set(HttpHeaders.RETRY_AFTER,
                    String.valueOf(evaluation.decision().retryAfterSeconds()));
        }
    }
}
