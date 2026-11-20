package com.example.ratelimiter.adapter.in.web.security;

import com.example.ratelimiter.adapter.in.web.error.ErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 403 in the uniform JSON error format. */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private final ErrorResponses errorResponses;

    public JsonAccessDeniedHandler(ErrorResponses errorResponses) {
        this.errorResponses = errorResponses;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        LOG.warn("Access denied for {} {}", request.getMethod(), request.getRequestURI(),
                exception);
        errorResponses.write(response, HttpStatus.FORBIDDEN, "Access denied", request);
    }
}
