package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/** A named set of target resources that can share one policy assignment. */
public record ResourceGroup(Long id, String name) {

    public ResourceGroup {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Resource group name is required");
        }
    }

    public static ResourceGroup newGroup(String name) {
        return new ResourceGroup(null, name);
    }
}
