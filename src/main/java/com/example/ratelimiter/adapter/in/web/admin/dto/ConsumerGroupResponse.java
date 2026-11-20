package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;

/** Consumer group representation. */
public record ConsumerGroupResponse(Long id, ConsumerGroupName groupName) {

    public static ConsumerGroupResponse from(ConsumerGroup group) {
        return new ConsumerGroupResponse(group.id(), group.name());
    }
}
