package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.application.port.in.command.CreateResourceCommand;
import com.example.ratelimiter.application.port.in.command.UpdateResourceCommand;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceFilter;

/** CRUD for target resources. */
public interface ManageResourcesUseCase {

    Resource create(CreateResourceCommand command);

    Resource update(Long id, UpdateResourceCommand command);

    /** Deletes a resource not referenced by any assignment. */
    void delete(Long id);

    Resource get(Long id);

    PageResult<Resource> list(ResourceFilter filter, PageQuery page);
}
