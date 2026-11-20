package com.example.ratelimiter.application.port.in.command;

import com.example.ratelimiter.domain.model.ConsumerGroupName;
import java.util.List;

/**
 * Creates a consumer group together with its first consumers: a group must never be empty.
 *
 * @param initialConsumerIds at least one consumer id
 */
public record CreateConsumerGroupCommand(ConsumerGroupName name, List<String> initialConsumerIds) {

    public CreateConsumerGroupCommand {
        initialConsumerIds = initialConsumerIds == null
                ? List.of() : List.copyOf(initialConsumerIds);
    }
}
