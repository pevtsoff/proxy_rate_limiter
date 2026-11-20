package com.example.ratelimiter.adapter.in.web.proxy;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.ProxiedRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Translates the servlet request into the transport-neutral {@link ProxiedRequest}.
 *
 * <p>The consumer's {@code Authorization} header is not forwarded: the proxy terminates
 * authentication and passes the verified identity as {@code X-Consumer-Id} instead (standard
 * API-gateway behaviour; it also keeps bearer tokens out of the target's request log).
 */
@Component
public class ProxiedRequestFactory {

    public ProxiedRequest from(HttpServletRequest request, String consumerId) throws IOException {
        return new ProxiedRequest(
                consumerId,
                HttpMethod.fromValue(request.getMethod()),
                pathOf(request),
                request.getQueryString(),
                headersOf(request),
                request.getInputStream().readAllBytes());
    }

    public static String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static Map<String, List<String>> headersOf(HttpServletRequest request) {
        Map<String, List<String>> headers = new HashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            if (!HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                headers.put(name, Collections.list(request.getHeaders(name)));
            }
        }
        headers.put("X-Forwarded-For", List.of(request.getRemoteAddr()));
        headers.put("X-Forwarded-Proto", List.of(request.getScheme()));
        headers.put("X-Forwarded-Host", List.of(request.getServerName()));
        return headers;
    }
}
