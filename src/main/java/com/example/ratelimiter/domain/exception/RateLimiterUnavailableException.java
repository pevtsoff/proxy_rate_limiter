package com.example.ratelimiter.domain.exception;

/**
 * The shared counter store cannot be reached. The proxy fails closed (HTTP 503) because
 * letting traffic through unmetered would defeat the purpose of the limiter.
 */
public class RateLimiterUnavailableException extends DomainException {

    public RateLimiterUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
