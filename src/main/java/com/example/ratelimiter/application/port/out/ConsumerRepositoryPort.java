package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import java.util.Optional;

/** Persistence of consumers. */
public interface ConsumerRepositoryPort {

    Consumer save(Consumer consumer);

    Optional<Consumer> findById(Long id);

    Optional<Consumer> findByConsumerId(String consumerId);

    PageResult<Consumer> findAll(ConsumerFilter filter, PageQuery page);

    long countByConsumerGroupId(Long consumerGroupId);

    void deleteById(Long id);

    void deleteByConsumerGroupId(Long consumerGroupId);
}
