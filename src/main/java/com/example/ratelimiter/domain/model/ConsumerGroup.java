package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/** A consumer tier; rate-limiting policies are only ever assigned to consumer groups. */
public record ConsumerGroup(Long id, ConsumerGroupName name) {

    public ConsumerGroup {
        if (name == null) {
            throw new ValidationException("group_name is required");
        }
    }

    public static ConsumerGroup newGroup(ConsumerGroupName name) {
        return new ConsumerGroup(null, name);
    }
}
