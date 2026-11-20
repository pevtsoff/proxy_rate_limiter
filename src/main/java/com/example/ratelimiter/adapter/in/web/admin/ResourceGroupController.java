package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.PageResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.ResourceGroupRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.ResourceGroupResponse;
import com.example.ratelimiter.application.port.in.ManageResourceGroupsUseCase;
import com.example.ratelimiter.domain.model.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD for resource groups. */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/resource-groups")
public class ResourceGroupController {

    private final ManageResourceGroupsUseCase resourceGroups;

    public ResourceGroupController(ManageResourceGroupsUseCase resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    @GetMapping
    public PageResponse<ResourceGroupResponse> list(
            @RequestParam(defaultValue = AdminApi.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AdminApi.DEFAULT_SIZE) int size) {
        return PageResponse.from(
                resourceGroups.list(new PageQuery(page, size)), ResourceGroupResponse::from);
    }

    @GetMapping("/{id}")
    public ResourceGroupResponse get(@PathVariable("id") Long id) {
        return ResourceGroupResponse.from(resourceGroups.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceGroupResponse create(@Valid @RequestBody ResourceGroupRequest request) {
        return ResourceGroupResponse.from(resourceGroups.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public ResourceGroupResponse update(
            @PathVariable("id") Long id, @Valid @RequestBody ResourceGroupRequest request) {
        return ResourceGroupResponse.from(resourceGroups.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        resourceGroups.delete(id);
    }
}
