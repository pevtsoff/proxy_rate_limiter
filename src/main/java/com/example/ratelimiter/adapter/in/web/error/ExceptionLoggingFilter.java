package com.example.ratelimiter.adapter.in.web.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Outermost filter: logs any exception that escapes the filter chain (i.e. was not handled by
 * {@link GlobalExceptionHandler}) and converts it into the uniform JSON error.
 */
public class ExceptionLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionLoggingFilter.class);

    private final ErrorResponses errorResponses;

    public ExceptionLoggingFilter(ErrorResponses errorResponses) {
        this.errorResponses = errorResponses;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            LOG.error("Unhandled exception for {} {}", request.getMethod(),
                    request.getRequestURI(), exception);
            if (!response.isCommitted()) {
                response.reset();
                errorResponses.write(response, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error", request);
            }
        }
    }
}
