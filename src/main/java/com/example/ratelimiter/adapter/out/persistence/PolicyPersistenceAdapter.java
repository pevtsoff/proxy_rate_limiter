package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.adapter.out.persistence.entity.PolicyEntity;
import com.example.ratelimiter.adapter.out.persistence.repository.PolicyJpaRepository;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyFilter;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link PolicyRepositoryPort} backed by PostgreSQL through Spring Data JPA. */
@Component
public class PolicyPersistenceAdapter implements PolicyRepositoryPort {

    private final PolicyJpaRepository repository;

    public PolicyPersistenceAdapter(PolicyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Policy save(Policy policy) {
        PolicyEntity entity = policy.id() == null ? PolicyEntity.from(policy) : load(policy.id());
        entity.apply(policy);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Policy> findById(Long id) {
        return repository.findById(id).map(PolicyEntity::toDomain);
    }

    @Override
    public Optional<Policy> findByName(String name) {
        return repository.findByName(name).map(PolicyEntity::toDomain);
    }

    @Override
    public Optional<Policy> findDefault() {
        return repository.findByDefaultPolicyTrue().map(PolicyEntity::toDomain);
    }

    @Override
    public PageResult<Policy> findAll(PolicyFilter filter, PageQuery page) {
        var specification = new Specifications<PolicyEntity>()
                .containsIgnoreCase("name", filter.nameContains())
                .equal("strategy", filter.strategy())
                .build();
        return Pages.toPageResult(
                repository.findAll(specification, Pages.toPageRequest(page)),
                        PolicyEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private PolicyEntity load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Policy", id));
    }
}
