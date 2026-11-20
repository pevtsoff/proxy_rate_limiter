package com.example.ratelimiter.domain.exception;

/** Input violates a domain invariant (maps to HTTP 400). */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }
}
