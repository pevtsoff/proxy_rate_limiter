package com.example.ratelimiter.domain.model;

/**
 * An enum whose external (API / database) representation is a stable string code
 * rather than the Java constant name, e.g. {@code moving-window} or {@code resource_group}.
 */
public interface CodedEnum {

    /**
     * Returns the external code of this constant.
     *
     * @return stable string code used in REST payloads and database columns
     */
    String value();
}
