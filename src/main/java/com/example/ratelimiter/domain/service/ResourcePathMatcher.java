package com.example.ratelimiter.domain.service;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourcePaths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Matches a concrete request path such as {@code /orders/42} against OpenAPI path templates such
 * as {@code /orders/{id}}.
 *
 * <p>When several templates match, the most specific one wins (the one with the most literal
 * segments), so {@code /orders/summary} beats {@code /orders/{id}} - the same rule OpenAPI and
 * Spring MVC use.
 */
public final class ResourcePathMatcher {

    private static final String SEPARATOR = "/";

    /** Returns the best matching resource for the method and path, if any. */
    public Optional<Resource> match(Collection<Resource> resources, HttpMethod method,
            String path) {
        String[] requestSegments = segments(ResourcePaths.normalize(path));
        return resources.stream()
                .filter(resource -> resource.method() == method)
                .filter(resource -> matches(segments(resource.path()), requestSegments))
                .max(Comparator.comparingInt((Resource resource) -> literalSegments(resource))
                        .thenComparing(Resource::id, Comparator.reverseOrder()));
    }

    private static boolean matches(String[] templateSegments, String[] requestSegments) {
        if (templateSegments.length != requestSegments.length) {
            return false;
        }
        for (int index = 0; index < templateSegments.length; index++) {
            if (!segmentMatches(templateSegments[index], requestSegments[index])) {
                return false;
            }
        }
        return true;
    }

    private static boolean segmentMatches(String templateSegment, String requestSegment) {
        return isVariable(templateSegment)
                ? !requestSegment.isEmpty()
                : templateSegment.equals(requestSegment);
    }

    private static int literalSegments(Resource resource) {
        int literals = 0;
        for (String segment : segments(resource.path())) {
            literals += isVariable(segment) ? 0 : 1;
        }
        return literals;
    }

    private static boolean isVariable(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String[] segments(String normalizedPath) {
        return SEPARATOR.equals(normalizedPath)
                ? new String[0]
                : normalizedPath.substring(1).split(SEPARATOR);
    }
}
