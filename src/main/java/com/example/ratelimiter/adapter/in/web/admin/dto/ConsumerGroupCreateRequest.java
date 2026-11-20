package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.CreateConsumerGroupCommand;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** A consumer group is created with its first consumers: a group can never be empty. */
public record ConsumerGroupCreateRequest(
        @NotNull ConsumerGroupName groupName,
        @NotEmpty List<@NotBlank String> consumerIds) {

    public CreateConsumerGroupCommand toCommand() {
        return new CreateConsumerGroupCommand(groupName, consumerIds);
    }
}
