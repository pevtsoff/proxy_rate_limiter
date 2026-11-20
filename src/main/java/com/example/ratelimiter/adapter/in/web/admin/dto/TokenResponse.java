package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.IssuedToken;
import java.time.Instant;

/** A bearer token for calling the proxy as a consumer. */
public record TokenResponse(
        String consumerId, String accessToken, String tokenType, Instant expiresAt) {

    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.consumerId(), token.token(), "Bearer", token.expiresAt());
    }
}
