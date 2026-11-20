package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/**
 * One endpoint of the target API, e.g. {@code GET /orders/{id}}.
 *
 * @param path            OpenAPI path template, normalized (see {@link ResourcePaths})
 * @param resourceGroupId optional owning resource group
 */
public record Resource(Long id, HttpMethod method, String path, IoType ioType,
        Long resourceGroupId) {

    public Resource {
        if (method == null) {
            throw new ValidationException("HTTP method is required");
        }
        path = ResourcePaths.normalize(path);
        ioType = ioType == null ? method.defaultIoType() : ioType;
    }

    /** Creates a resource discovered from the target's OpenAPI document with default io type. */
    public static Resource discovered(HttpMethod method, String path, Long resourceGroupId) {
        return new Resource(null, method, path, method.defaultIoType(), resourceGroupId);
    }

    public Resource withClassification(IoType newIoType, Long newResourceGroupId) {
        return new Resource(id, method, path, newIoType, newResourceGroupId);
    }

    public boolean belongsTo(Long groupId) {
        return resourceGroupId != null && resourceGroupId.equals(groupId);
    }

    /** Human-readable identity, e.g. {@code GET /orders/{id}}. */
    public String label() {
        return method.value() + " " + path;
    }
}
