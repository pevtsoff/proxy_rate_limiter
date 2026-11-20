package com.example.ratelimiter.domain.model;

/** An operation found in the target API's OpenAPI document. */
public record DiscoveredEndpoint(HttpMethod method, String path) {

    public DiscoveredEndpoint {
        path = ResourcePaths.normalize(path);
    }

    public boolean sameEndpointAs(Resource resource) {
        return method == resource.method() && path.equals(resource.path());
    }
}
