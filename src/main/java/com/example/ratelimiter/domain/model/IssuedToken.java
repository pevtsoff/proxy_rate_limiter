package com.example.ratelimiter.domain.model;

import java.time.Instant;

/** A bearer token minted for a registered consumer. */
public record IssuedToken(String consumerId, String token, Instant expiresAt) {
}
