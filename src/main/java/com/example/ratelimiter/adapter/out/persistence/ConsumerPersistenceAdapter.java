package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.adapter.out.persistence.entity.ConsumerEntity;
import com.example.ratelimiter.adapter.out.persistence.repository.ConsumerJpaRepository;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link ConsumerRepositoryPort} backed by PostgreSQL. */
@Component
public class ConsumerPersistenceAdapter implements ConsumerRepositoryPort {

    private final ConsumerJpaRepository repository;

    public ConsumerPersistenceAdapter(ConsumerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Consumer save(Consumer consumer) {
        ConsumerEntity entity = consumer.id() == null
                ? ConsumerEntity.from(consumer)
                : repository.findById(consumer.id())
                        .orElseThrow(() -> new NotFoundException("Consumer", consumer.id()));
        entity.apply(consumer);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Consumer> findById(Long id) {
        return repository.findById(id).map(ConsumerEntity::toDomain);
    }

    @Override
    public Optional<Consumer> findByConsumerId(String consumerId) {
        return repository.findByConsumerId(consumerId).map(ConsumerEntity::toDomain);
    }

    @Override
    public PageResult<Consumer> findAll(ConsumerFilter filter, PageQuery page) {
        var specification = new Specifications<ConsumerEntity>()
                .containsIgnoreCase("consumerId", filter.consumerIdContains())
                .equal("consumerGroupId", filter.consumerGroupId())
                .build();
        return Pages.toPageResult(repository.findAll(specification, Pages.toPageRequest(page)),
                ConsumerEntity::toDomain);
    }

    @Override
    public long countByConsumerGroupId(Long consumerGroupId) {
        return repository.countByConsumerGroupId(consumerGroupId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByConsumerGroupId(Long consumerGroupId) {
        repository.deleteByConsumerGroupId(consumerGroupId);
    }
}
