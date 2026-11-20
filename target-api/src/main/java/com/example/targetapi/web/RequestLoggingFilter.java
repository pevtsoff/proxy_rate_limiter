package com.example.targetapi.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs ALL incoming requests (method, path, query, consumer, client, status, duration), which
 * makes it easy to verify that rejected requests never reach the target.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            LOG.info("{} {}{} consumer={} forwardedFor={} contentLength={} -> {} in {} ms",
                    request.getMethod(), request.getRequestURI(), queryOf(request),
                    request.getHeader("X-Consumer-Id"), request.getHeader("X-Forwarded-For"),
                    request.getContentLengthLong(), response.getStatus(),
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    private static String queryOf(HttpServletRequest request) {
        return request.getQueryString() == null ? "" : "?" + request.getQueryString();
    }
}
