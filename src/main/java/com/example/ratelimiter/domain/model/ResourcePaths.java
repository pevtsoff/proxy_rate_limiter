package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/** Canonical form of REST paths so that {@code /orders/}, {@code orders} and {@code /orders} match. */
public final class ResourcePaths {

    private static final String SEPARATOR = "/";

    private ResourcePaths() {
    }

    /** Returns the path with a single leading slash, no trailing slash and no empty segments. */
    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            throw new ValidationException("Resource path is required");
        }
        String withoutQuery = stripQuery(path.trim());
        String collapsed = withoutQuery.replaceAll("/{2,}", SEPARATOR);
        String withLeadingSlash = collapsed.startsWith(SEPARATOR)
                ? collapsed : SEPARATOR + collapsed;
        return stripTrailingSlash(withLeadingSlash);
    }

    /** Returns the first path segment, used as the default resource-group name. */
    public static String firstSegment(String path) {
        String normalized = normalize(path);
        String[] segments = normalized.substring(1).split(SEPARATOR);
        return segments[0].isEmpty() ? "root" : segments[0];
    }

    /** Returns the path without its leading slash, as shown in user-facing error payloads. */
    public static String withoutLeadingSlash(String path) {
        return path.startsWith(SEPARATOR) ? path.substring(1) : path;
    }

    private static String stripQuery(String path) {
        int queryStart = path.indexOf('?');
        return queryStart >= 0 ? path.substring(0, queryStart) : path;
    }

    private static String stripTrailingSlash(String path) {
        if (path.length() > 1 && path.endsWith(SEPARATOR)) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
