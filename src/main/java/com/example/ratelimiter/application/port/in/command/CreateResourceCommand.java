package com.example.ratelimiter.application.port.in.command;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;

/**
 * Registers a resource manually (normally resources come from the OpenAPI sync).
 *
 * @param ioType optional; defaults from the HTTP method
 */
public record CreateResourceCommand(
        HttpMethod method, String path, IoType ioType, Long resourceGroupId) {
}
