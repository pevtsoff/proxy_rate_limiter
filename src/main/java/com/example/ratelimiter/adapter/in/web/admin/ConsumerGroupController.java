package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.ConsumerGroupCreateRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.ConsumerGroupResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.ConsumerGroupUpdateRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.PageResponse;
import com.example.ratelimiter.application.port.in.ManageConsumerGroupsUseCase;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.example.ratelimiter.domain.model.PageQuery;
import jakarta.validation.Valid;
import java.util.List;
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

/** MUC1.1: CRUD for consumer groups. */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/consumer-groups")
public class ConsumerGroupController {

    private final ManageConsumerGroupsUseCase groups;

    public ConsumerGroupController(ManageConsumerGroupsUseCase groups) {
        this.groups = groups;
    }

    @GetMapping
    public PageResponse<ConsumerGroupResponse> list(
            @RequestParam(defaultValue = AdminApi.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AdminApi.DEFAULT_SIZE) int size) {
        return PageResponse.from(groups.list(new PageQuery(page, size)),
                ConsumerGroupResponse::from);
    }

    @GetMapping("/names")
    public List<ConsumerGroupName> names() {
        return List.of(ConsumerGroupName.values());
    }

    @GetMapping("/{id}")
    public ConsumerGroupResponse get(@PathVariable("id") Long id) {
        return ConsumerGroupResponse.from(groups.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsumerGroupResponse create(@Valid @RequestBody ConsumerGroupCreateRequest request) {
        return ConsumerGroupResponse.from(groups.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public ConsumerGroupResponse update(
            @PathVariable("id") Long id, @Valid @RequestBody ConsumerGroupUpdateRequest request) {
        return ConsumerGroupResponse.from(groups.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        groups.delete(id);
    }
}
