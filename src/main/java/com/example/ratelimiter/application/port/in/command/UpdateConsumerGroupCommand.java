package com.example.ratelimiter.application.port.in.command;

import com.example.ratelimiter.domain.model.ConsumerGroupName;

/** Renames a consumer group to another tier. */
public record UpdateConsumerGroupCommand(ConsumerGroupName name) {
}
