package com.example.ratelimiter.domain.exception;

/** Operation contradicts current state or a business rule (maps to HTTP 409). */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
