package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import com.example.ratelimiter.domain.model.Resource;

/** Resource representation. */
public record ResourceResponse(
        Long id, HttpMethod method, String resourcePath, IoType ioType, Long resourceGroupId) {

    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(resource.id(), resource.method(), resource.path(),
                resource.ioType(), resource.resourceGroupId());
    }
}
