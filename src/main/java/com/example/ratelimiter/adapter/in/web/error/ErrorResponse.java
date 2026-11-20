package com.example.ratelimiter.adapter.in.web.error;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The single JSON error format of the proxy, e.g.
 * {@code {"error": "...", "resource": "orders/1", "https_status_code": 429}}.
 */
public record ErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("resource") String resource,
        @JsonProperty("https_status_code") int httpsStatusCode) {
}
