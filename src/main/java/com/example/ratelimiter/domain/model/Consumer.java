package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/**
 * An API caller identified by the {@code sub} claim of its bearer token.
 *
 * @param consumerId      external user id carried in the token
 * @param consumerGroupId mandatory: a consumer cannot exist without a consumer group
 */
public record Consumer(Long id, String consumerId, Long consumerGroupId) {

    public Consumer {
        if (consumerId == null || consumerId.isBlank()) {
            throw new ValidationException("consumer_id is required");
        }
        if (consumerGroupId == null) {
            throw new ValidationException("A consumer must belong to a consumer group");
        }
        consumerId = consumerId.trim();
    }

    public static Consumer newConsumer(String consumerId, Long consumerGroupId) {
        return new Consumer(null, consumerId, consumerGroupId);
    }

    public Consumer withDetails(String newConsumerId, Long newConsumerGroupId) {
        return new Consumer(id, newConsumerId, newConsumerGroupId);
    }
}
