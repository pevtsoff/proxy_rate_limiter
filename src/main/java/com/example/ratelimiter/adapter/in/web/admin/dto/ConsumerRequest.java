package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.ConsumerCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Create / update payload of a consumer. */
public record ConsumerRequest(
        @NotBlank @Size(max = 128) String consumerId, @NotNull Long consumerGroupId) {

    public ConsumerCommand toCommand() {
        return new ConsumerCommand(consumerId, consumerGroupId);
    }
}
