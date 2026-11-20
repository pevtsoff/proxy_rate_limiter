package com.example.ratelimiter.adapter.out.http;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Headers that describe a single connection (RFC 9110 section 7.6.1) or that the HTTP client /
 * servlet container computes itself; they must not be copied between the two legs of a proxy.
 */
public final class HopByHopHeaders {

    private static final Set<String> NOT_FORWARDED = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te",
            "trailer", "transfer-encoding", "upgrade", "host", "content-length", "expect");

    private HopByHopHeaders() {
    }

    public static boolean isForwardable(String headerName) {
        return !NOT_FORWARDED.contains(headerName.toLowerCase(Locale.ROOT))
                && !headerName.startsWith(":");
    }

    public static Map<String, List<String>> forwardableOnly(Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .filter(entry -> isForwardable(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> first));
    }
}
