package com.example.ratelimiter.adapter.in.web.security;

import com.example.ratelimiter.adapter.in.web.error.ErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 401 in the uniform JSON error format (missing / invalid credentials). */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private final ErrorResponses errorResponses;
    private final String challenge;

    /**
     * @param challenge value of the {@code WWW-Authenticate} header, e.g. {@code Bearer}
     */
    public JsonAuthenticationEntryPoint(ErrorResponses errorResponses, String challenge) {
        this.errorResponses = errorResponses;
        this.challenge = challenge;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        LOG.warn("Authentication failed for {} {}", request.getMethod(), request.getRequestURI(),
                exception);
        response.setHeader("WWW-Authenticate", challenge);
        errorResponses.write(response, HttpStatus.UNAUTHORIZED,
                "Authentication required: " + exception.getMessage(), request);
    }
}
