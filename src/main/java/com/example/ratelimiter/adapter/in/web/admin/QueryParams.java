package com.example.ratelimiter.adapter.in.web.admin;

import java.util.function.Function;

/** Parses optional query parameters into domain types. */
final class QueryParams {

    private QueryParams() {
    }

    static <T> T optional(String value, Function<String, T> parser) {
        return value == null || value.isBlank() ? null : parser.apply(value);
    }
}
