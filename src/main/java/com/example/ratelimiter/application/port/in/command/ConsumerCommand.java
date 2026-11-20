package com.example.ratelimiter.application.port.in.command;

/** Data needed to create or update a consumer. */
public record ConsumerCommand(String consumerId, Long consumerGroupId) {
}
