package com.example.ratelimiter.domain.model;

import java.util.List;
import java.util.Map;

/**
 * A consumer request received by the proxy, in transport-neutral form.
 *
 * @param rawQuery query string without the leading {@code ?}, or {@code null}
 * @param headers  request headers (multi-valued)
 * @param body     request body, empty array when there is none
 */
public record ProxiedRequest(
        String consumerId,
        HttpMethod method,
        String path,
        String rawQuery,
        Map<String, List<String>> headers,
        byte[] body) {

    public ProxiedRequest {
        headers = Map.copyOf(headers);
        body = body == null ? new byte[0] : body;
    }

    /** Path plus query string, as it must be sent to the target API. */
    public String pathWithQuery() {
        return rawQuery == null || rawQuery.isEmpty() ? path : path + "?" + rawQuery;
    }
}
