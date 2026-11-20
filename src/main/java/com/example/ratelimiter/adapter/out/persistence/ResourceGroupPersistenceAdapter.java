package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.adapter.out.persistence.entity.ResourceGroupEntity;
import com.example.ratelimiter.adapter.out.persistence.repository.ResourceGroupJpaRepository;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.ResourceGroup;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link ResourceGroupRepositoryPort} backed by PostgreSQL. */
@Component
public class ResourceGroupPersistenceAdapter implements ResourceGroupRepositoryPort {

    private final ResourceGroupJpaRepository repository;

    public ResourceGroupPersistenceAdapter(ResourceGroupJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ResourceGroup save(ResourceGroup group) {
        ResourceGroupEntity entity = group.id() == null
                ? ResourceGroupEntity.from(group)
                : repository.findById(group.id())
                        .orElseThrow(() -> new NotFoundException("Resource group", group.id()));
        entity.apply(group);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<ResourceGroup> findById(Long id) {
        return repository.findById(id).map(ResourceGroupEntity::toDomain);
    }

    @Override
    public Optional<ResourceGroup> findByName(String name) {
        return repository.findByName(name).map(ResourceGroupEntity::toDomain);
    }

    @Override
    public PageResult<ResourceGroup> findAll(PageQuery page) {
        return Pages.toPageResult(
                repository.findAll(Pages.toPageRequest(page)), ResourceGroupEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
