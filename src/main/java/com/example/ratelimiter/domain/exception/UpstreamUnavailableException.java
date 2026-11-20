package com.example.ratelimiter.domain.exception;

/** The target API could not be reached or answered with a broken response (HTTP 502). */
public class UpstreamUnavailableException extends DomainException {

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
