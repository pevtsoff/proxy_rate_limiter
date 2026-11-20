package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.PageResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.ResourceCreateRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.ResourceResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.ResourceUpdateRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.SyncReportResponse;
import com.example.ratelimiter.application.port.in.ManageResourcesUseCase;
import com.example.ratelimiter.application.port.in.SyncResourcesUseCase;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.ResourceFilter;
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

/** MUC1.4: target resources, including the OpenAPI synchronisation endpoint. */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/resources")
public class ResourceController {

    private final ManageResourcesUseCase resources;
    private final SyncResourcesUseCase sync;

    public ResourceController(ManageResourcesUseCase resources, SyncResourcesUseCase sync) {
        this.resources = resources;
        this.sync = sync;
    }

    @GetMapping
    public PageResponse<ResourceResponse> list(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String method,
            @RequestParam(name = "io_type", required = false) String ioType,
            @RequestParam(name = "resource_group_id", required = false) Long resourceGroupId,
            @RequestParam(defaultValue = AdminApi.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AdminApi.DEFAULT_SIZE) int size) {
        ResourceFilter filter = new ResourceFilter(path,
                QueryParams.optional(method, HttpMethod::fromValue),
                QueryParams.optional(ioType, IoType::fromValue), resourceGroupId);
        return PageResponse.from(
                resources.list(filter, new PageQuery(page, size)), ResourceResponse::from);
    }

    /** Fetches the target API's OpenAPI document and imports new endpoints. */
    @PostMapping("/sync")
    public SyncReportResponse synchronize() {
        return SyncReportResponse.from(sync.synchronize());
    }

    @GetMapping("/{id}")
    public ResourceResponse get(@PathVariable("id") Long id) {
        return ResourceResponse.from(resources.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse create(@Valid @RequestBody ResourceCreateRequest request) {
        return ResourceResponse.from(resources.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public ResourceResponse update(
            @PathVariable("id") Long id, @Valid @RequestBody ResourceUpdateRequest request) {
        return ResourceResponse.from(resources.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        resources.delete(id);
    }
}
