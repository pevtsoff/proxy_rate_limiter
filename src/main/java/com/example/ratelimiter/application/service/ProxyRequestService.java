package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.EvaluateRequestUseCase;
import com.example.ratelimiter.application.port.in.ForwardedOutcome;
import com.example.ratelimiter.application.port.in.ProxyOutcome;
import com.example.ratelimiter.application.port.in.ProxyRequestUseCase;
import com.example.ratelimiter.application.port.in.RejectedOutcome;
import com.example.ratelimiter.application.port.out.UpstreamForwarderPort;
import com.example.ratelimiter.domain.model.ProxiedRequest;
import com.example.ratelimiter.domain.model.RequestEvaluation;

/** CUC1: forward the request when capacity is available, otherwise reject it. */
public final class ProxyRequestService implements ProxyRequestUseCase {

    private final EvaluateRequestUseCase evaluator;
    private final UpstreamForwarderPort forwarder;

    public ProxyRequestService(EvaluateRequestUseCase evaluator, UpstreamForwarderPort forwarder) {
        this.evaluator = evaluator;
        this.forwarder = forwarder;
    }

    @Override
    public ProxyOutcome handle(ProxiedRequest request) {
        RequestEvaluation evaluation =
                evaluator.evaluate(request.consumerId(), request.method(), request.path());
        if (!evaluation.allowed()) {
            return new RejectedOutcome(evaluation);
        }
        return new ForwardedOutcome(evaluation, forwarder.forward(request));
    }
}
