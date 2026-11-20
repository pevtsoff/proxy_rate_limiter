package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.IssuedToken;

/** Mints signed bearer tokens. */
public interface TokenIssuerPort {

    IssuedToken issue(String consumerId);
}
