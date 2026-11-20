package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.CreateResourceCommand;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Manual registration of a resource.
 *
 * @param ioType optional, derived from the method when omitted
 */
public record ResourceCreateRequest(
        @NotNull HttpMethod method,
        @NotBlank @Size(max = 512) String resourcePath,
        IoType ioType,
        Long resourceGroupId) {

    public CreateResourceCommand toCommand() {
        return new CreateResourceCommand(method, resourcePath, ioType, resourceGroupId);
    }
}
