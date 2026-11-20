package com.example.ratelimiter.adapter.in.web.proxy;

import com.example.ratelimiter.adapter.in.web.error.ErrorResponse;
import com.example.ratelimiter.application.port.in.ForwardedOutcome;
import com.example.ratelimiter.application.port.in.ProxyOutcome;
import com.example.ratelimiter.application.port.in.ProxyRequestUseCase;
import com.example.ratelimiter.application.port.in.RejectedOutcome;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.ProxiedRequest;
import com.example.ratelimiter.domain.model.ResourcePaths;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CUC1: catch-all endpoint. Every path that is not reserved for the proxy itself is evaluated
 * against the consumer's rate limit and, if allowed, forwarded to the target API unchanged.
 */
@RestController
public class ProxyController {

    static final String LIMIT_EXCEEDED = "You exceeded the request capacity for the current period";

    private static final Logger LOG = LoggerFactory.getLogger(ProxyController.class);

    private final ProxyRequestUseCase proxy;
    private final ProxiedRequestFactory requestFactory;

    public ProxyController(ProxyRequestUseCase proxy, ProxiedRequestFactory requestFactory) {
        this.proxy = proxy;
        this.requestFactory = requestFactory;
    }

    @RequestMapping("/**")
    public ResponseEntity<Object> handle(
            HttpServletRequest servletRequest,
                    @AuthenticationPrincipal Jwt jwt) throws IOException {
        String path = ProxiedRequestFactory.pathOf(servletRequest);
        if (jwt == null || ReservedPaths.isReserved(path)) {
            throw new NotFoundException("Route", path);
        }
        ProxiedRequest request = requestFactory.from(servletRequest, jwt.getSubject());
        ProxyOutcome outcome = proxy.handle(request);
        logOutcome(request, outcome);
        return switch (outcome) {
            case ForwardedOutcome forwarded -> forwardedResponse(forwarded);
            case RejectedOutcome rejected -> rejectedResponse(rejected, request.path());
        };
    }

    private static ResponseEntity<Object> forwardedResponse(ForwardedOutcome outcome) {
        HttpHeaders headers = new HttpHeaders();
        outcome.response().headers().forEach(headers::addAll);
        RateLimitHeaders.apply(outcome.evaluation(), headers);
        return ResponseEntity.status(outcome.response().status())
                .headers(headers)
                .body(outcome.response().body());
    }

    private static ResponseEntity<Object> rejectedResponse(RejectedOutcome outcome, String path) {
        HttpHeaders headers = new HttpHeaders();
        RateLimitHeaders.apply(outcome.evaluation(), headers);
        ErrorResponse body = new ErrorResponse(LIMIT_EXCEEDED,
                ResourcePaths.withoutLeadingSlash(path), HttpStatus.TOO_MANY_REQUESTS.value());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private static void logOutcome(ProxiedRequest request, ProxyOutcome outcome) {
        if (outcome instanceof RejectedOutcome rejected) {
            LOG.warn("429 {} {} consumer={} scope={} retryAfter={}s", request.method(),
                    request.path(), request.consumerId(), rejected.evaluation().rule().scope(),
                    rejected.evaluation().decision().retryAfterSeconds());
        } else if (outcome instanceof ForwardedOutcome forwarded) {
            LOG.info("{} {} {} consumer={} scope={} remaining={}", forwarded.response().status(),
                    request.method(), request.path(), request.consumerId(),
                    forwarded.evaluation().rule().scope(),
                    forwarded.evaluation().decision().remaining());
        }
    }
}
