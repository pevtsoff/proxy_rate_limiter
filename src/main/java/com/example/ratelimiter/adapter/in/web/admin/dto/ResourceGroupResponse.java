package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.ResourceGroup;

/** Resource group representation. */
public record ResourceGroupResponse(Long id, String name) {

    public static ResourceGroupResponse from(ResourceGroup group) {
        return new ResourceGroupResponse(group.id(), group.name());
    }
}
