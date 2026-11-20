package com.example.ratelimiter.domain.exception;

/** A syntactically valid token names a consumer that is not registered (maps to HTTP 403). */
public class UnknownConsumerException extends DomainException {

    public UnknownConsumerException(String consumerId) {
        super("Consumer '" + consumerId + "' is not registered");
    }
}
