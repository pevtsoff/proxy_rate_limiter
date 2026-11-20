package com.example.ratelimiter.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ratelimiter.application.port.in.ForwardedOutcome;
import com.example.ratelimiter.application.port.in.ProxyOutcome;
import com.example.ratelimiter.application.port.in.RejectedOutcome;
import com.example.ratelimiter.application.port.in.ResolveApplicableRuleUseCase;
import com.example.ratelimiter.application.port.out.RateLimiterPort;
import com.example.ratelimiter.application.port.out.UpstreamForwarderPort;
import com.example.ratelimiter.application.service.EvaluateRequestService;
import com.example.ratelimiter.application.service.ProxyRequestService;
import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.ProxiedRequest;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.UpstreamResponse;
import com.example.ratelimiter.unit.support.TestData;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CUC1.4-6: forward when capacity is left, reject otherwise, never call the target twice. */
@ExtendWith(MockitoExtension.class)
class ProxyRequestServiceTest {

    private static final ProxiedRequest REQUEST = new ProxiedRequest(
            "alice", HttpMethod.GET, "/orders/1", null, Map.of(), new byte[0]);

    @Mock
    private ResolveApplicableRuleUseCase resolver;
    @Mock
    private RateLimiterPort rateLimiter;
    @Mock
    private UpstreamForwarderPort forwarder;

    private ProxyRequestService service;

    @BeforeEach
    void setUp() {
        service = new ProxyRequestService(new EvaluateRequestService(resolver, rateLimiter),
                forwarder);
        when(resolver.resolve("alice", HttpMethod.GET, "/orders/1"))
                .thenReturn(ApplicableRule.defaultForUnmatched(TestData.defaultPolicy()));
    }

    @Test
    void forwardsWhenCapacityIsAvailable() {
        when(rateLimiter.tryConsume(eq("rl:alice:default:unmatched:1-moving-window-60-minute"),
                any())).thenReturn(RateLimitDecision.allowed(60, 59));
        when(forwarder.forward(REQUEST)).thenReturn(new UpstreamResponse(200, Map.of(),
                new byte[0]));

        ProxyOutcome outcome = service.handle(REQUEST);

        assertThat(outcome).isInstanceOf(ForwardedOutcome.class);
        assertThat(outcome.evaluation().decision().remaining()).isEqualTo(59);
    }

    @Test
    void rejectsWithoutCallingTheTargetWhenLimitIsExhausted() {
        when(rateLimiter.tryConsume(anyString(), any()))
                .thenReturn(RateLimitDecision.rejected(60, Duration.ofSeconds(12)));

        ProxyOutcome outcome = service.handle(REQUEST);

        assertThat(outcome).isInstanceOf(RejectedOutcome.class);
        assertThat(outcome.evaluation().decision().retryAfterSeconds()).isEqualTo(12);
        verify(forwarder, never()).forward(any());
    }
}
