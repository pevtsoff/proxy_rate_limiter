package com.example.ratelimiter.adapter.in.web.proxy;

import java.util.List;

/** Path prefixes served by the proxy itself and therefore never forwarded. */
public final class ReservedPaths {

    public static final List<String> PREFIXES = List.of(
            "/admin", "/ui", "/VAADIN", "/login", "/logout", "/actuator", "/error");

    private ReservedPaths() {
    }

    public static boolean isReserved(String path) {
        return PREFIXES.stream().anyMatch(prefix -> path.equals(prefix)
                || path.startsWith(prefix + "/"));
    }
}
