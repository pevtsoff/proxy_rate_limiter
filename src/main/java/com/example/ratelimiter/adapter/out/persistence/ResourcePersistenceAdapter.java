package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.adapter.out.persistence.entity.ResourceEntity;
import com.example.ratelimiter.adapter.out.persistence.repository.ResourceJpaRepository;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceFilter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** {@link ResourceRepositoryPort} backed by PostgreSQL. */
@Component
public class ResourcePersistenceAdapter implements ResourceRepositoryPort {

    private final ResourceJpaRepository repository;

    public ResourcePersistenceAdapter(ResourceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Resource save(Resource resource) {
        ResourceEntity entity = resource.id() == null
                ? ResourceEntity.from(resource)
                : repository.findById(resource.id())
                        .orElseThrow(() -> new NotFoundException("Resource", resource.id()));
        entity.apply(resource);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Resource> findById(Long id) {
        return repository.findById(id).map(ResourceEntity::toDomain);
    }

    @Override
    public Optional<Resource> findByMethodAndPath(HttpMethod method, String path) {
        return repository.findByMethodAndPath(method, path).map(ResourceEntity::toDomain);
    }

    @Override
    public List<Resource> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(ResourceEntity::toDomain).toList();
    }

    @Override
    public List<Resource> findAllByIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(ResourceEntity::toDomain).toList();
    }

    @Override
    public PageResult<Resource> findAll(ResourceFilter filter, PageQuery page) {
        var specification = new Specifications<ResourceEntity>()
                .containsIgnoreCase("path", filter.pathContains())
                .equal("method", filter.method())
                .equal("ioType", filter.ioType())
                .equal("resourceGroupId", filter.resourceGroupId())
                .build();
        return Pages.toPageResult(repository.findAll(specification, Pages.toPageRequest(page)),
                ResourceEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
