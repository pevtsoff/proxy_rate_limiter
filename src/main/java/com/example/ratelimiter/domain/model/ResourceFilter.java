package com.example.ratelimiter.domain.model;

/** Optional criteria for listing resources; {@code null} fields are ignored. */
public record ResourceFilter(
        String pathContains, HttpMethod method, IoType ioType, Long resourceGroupId) {

    public static ResourceFilter none() {
        return new ResourceFilter(null, null, null, null);
    }
}
