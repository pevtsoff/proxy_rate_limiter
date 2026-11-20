package com.example.ratelimiter.domain.exception;

/** Referenced entity does not exist (maps to HTTP 404). */
public class NotFoundException extends DomainException {

    public NotFoundException(String entity, Object id) {
        super(entity + " '" + id + "' was not found");
    }
}
