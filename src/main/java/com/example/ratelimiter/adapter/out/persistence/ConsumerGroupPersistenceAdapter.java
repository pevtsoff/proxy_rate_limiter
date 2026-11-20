package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.adapter.out.persistence.entity.ConsumerGroupEntity;
import com.example.ratelimiter.adapter.out.persistence.repository.ConsumerGroupJpaRepository;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link ConsumerGroupRepositoryPort} backed by PostgreSQL. */
@Component
public class ConsumerGroupPersistenceAdapter implements ConsumerGroupRepositoryPort {

    private final ConsumerGroupJpaRepository repository;

    public ConsumerGroupPersistenceAdapter(ConsumerGroupJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ConsumerGroup save(ConsumerGroup group) {
        ConsumerGroupEntity entity = group.id() == null
                ? ConsumerGroupEntity.from(group)
                : repository.findById(group.id())
                        .orElseThrow(() -> new NotFoundException("Consumer group", group.id()));
        entity.apply(group);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<ConsumerGroup> findById(Long id) {
        return repository.findById(id).map(ConsumerGroupEntity::toDomain);
    }

    @Override
    public Optional<ConsumerGroup> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(ConsumerGroupEntity::toDomain);
    }

    @Override
    public Optional<ConsumerGroup> findByName(ConsumerGroupName name) {
        return repository.findByName(name).map(ConsumerGroupEntity::toDomain);
    }

    @Override
    public PageResult<ConsumerGroup> findAll(PageQuery page) {
        return Pages.toPageResult(
                repository.findAll(Pages.toPageRequest(page)), ConsumerGroupEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
