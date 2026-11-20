package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.ResourceGroupCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create / update payload of a resource group. */
public record ResourceGroupRequest(@NotBlank @Size(max = 100) String name) {

    public ResourceGroupCommand toCommand() {
        return new ResourceGroupCommand(name);
    }
}
