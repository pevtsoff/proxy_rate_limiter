package com.example.ratelimiter.adapter.in.web.error;

import com.example.ratelimiter.domain.model.ResourcePaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Builds {@link ErrorResponse}s and writes them directly to the servlet response for code that
 * runs outside Spring MVC (servlet filters, Spring Security handlers).
 */
@Component
public class ErrorResponses {

    private final ObjectMapper objectMapper;

    public ErrorResponses(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static ErrorResponse of(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(message, resourceOf(request), status.value());
    }

    /** Request path relative to the proxy root, without leading slash, e.g. {@code orders/1}. */
    public static String resourceOf(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ResourcePaths.withoutLeadingSlash(path);
    }

    public void write(HttpServletResponse response, HttpStatus status, String message,
            HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), of(status, message, request));
    }
}
