package com.example.ratelimiter.domain.model;

import java.util.List;
import java.util.Map;

/** Response of the target API that is relayed back to the consumer. */
public record UpstreamResponse(int status, Map<String, List<String>> headers, byte[] body) {

    public UpstreamResponse {
        headers = Map.copyOf(headers);
        body = body == null ? new byte[0] : body;
    }
}
