package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceFilter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence of target resources. */
public interface ResourceRepositoryPort {

    Resource save(Resource resource);

    Optional<Resource> findById(Long id);

    Optional<Resource> findByMethodAndPath(HttpMethod method, String path);

    /** Returns the whole catalogue; it is small (one row per target endpoint). */
    List<Resource> findAll();

    List<Resource> findAllByIds(Collection<Long> ids);

    PageResult<Resource> findAll(ResourceFilter filter, PageQuery page);

    void deleteById(Long id);
}
