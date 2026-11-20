package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.UpdateResourceCommand;
import com.example.ratelimiter.domain.model.IoType;
import jakarta.validation.constraints.NotNull;

/** Reclassification of a resource; {@code resourceGroupId = null} removes it from its group. */
public record ResourceUpdateRequest(@NotNull IoType ioType, Long resourceGroupId) {

    public UpdateResourceCommand toCommand() {
        return new UpdateResourceCommand(ioType, resourceGroupId);
    }
}
