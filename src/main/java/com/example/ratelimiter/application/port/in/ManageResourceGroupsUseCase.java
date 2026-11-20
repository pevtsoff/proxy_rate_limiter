package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.application.port.in.command.ResourceGroupCommand;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.ResourceGroup;

/** CRUD for resource groups. */
public interface ManageResourceGroupsUseCase {

    ResourceGroup create(ResourceGroupCommand command);

    ResourceGroup update(Long id, ResourceGroupCommand command);

    /** Deletes a group not referenced by any assignment; its resources become ungrouped. */
    void delete(Long id);

    ResourceGroup get(Long id);

    PageResult<ResourceGroup> list(PageQuery page);
}
