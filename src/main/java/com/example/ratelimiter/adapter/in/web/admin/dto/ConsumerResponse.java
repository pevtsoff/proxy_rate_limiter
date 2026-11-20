package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.Consumer;

/** Consumer representation. */
public record ConsumerResponse(Long id, String consumerId, Long consumerGroupId) {

    public static ConsumerResponse from(Consumer consumer) {
        return new ConsumerResponse(consumer.id(), consumer.consumerId(),
                consumer.consumerGroupId());
    }
}
