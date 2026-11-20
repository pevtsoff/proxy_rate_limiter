package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.UpdateConsumerGroupCommand;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import jakarta.validation.constraints.NotNull;

/** Renames a consumer group. */
public record ConsumerGroupUpdateRequest(@NotNull ConsumerGroupName groupName) {

    public UpdateConsumerGroupCommand toCommand() {
        return new UpdateConsumerGroupCommand(groupName);
    }
}
