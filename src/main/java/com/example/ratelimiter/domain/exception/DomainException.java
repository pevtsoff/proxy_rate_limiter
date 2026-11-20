package com.example.ratelimiter.domain.exception;

/** Base type of every business error raised by the domain and application layers. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
