package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.ResourceGroup;
import java.util.Optional;

/** Persistence of resource groups. */
public interface ResourceGroupRepositoryPort {

    ResourceGroup save(ResourceGroup group);

    Optional<ResourceGroup> findById(Long id);

    Optional<ResourceGroup> findByName(String name);

    PageResult<ResourceGroup> findAll(PageQuery page);

    void deleteById(Long id);
}
