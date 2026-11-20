package com.example.ratelimiter.domain.exception;

/** The target API did not answer within the configured timeout (HTTP 504). */
public class UpstreamTimeoutException extends DomainException {

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
