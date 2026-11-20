package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.ConsumerRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.ConsumerResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.PageResponse;
import com.example.ratelimiter.application.port.in.ManageConsumersUseCase;
import com.example.ratelimiter.domain.model.ConsumerFilter;
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

/** MUC1.2: CRUD for consumers. */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/consumers")
public class ConsumerController {

    private final ManageConsumersUseCase consumers;

    public ConsumerController(ManageConsumersUseCase consumers) {
        this.consumers = consumers;
    }

    @GetMapping
    public PageResponse<ConsumerResponse> list(
            @RequestParam(name = "consumer_id", required = false) String consumerId,
            @RequestParam(name = "consumer_group_id", required = false) Long consumerGroupId,
            @RequestParam(defaultValue = AdminApi.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AdminApi.DEFAULT_SIZE) int size) {
        ConsumerFilter filter = new ConsumerFilter(consumerId, consumerGroupId);
        return PageResponse.from(
                consumers.list(filter, new PageQuery(page, size)), ConsumerResponse::from);
    }

    @GetMapping("/{id}")
    public ConsumerResponse get(@PathVariable("id") Long id) {
        return ConsumerResponse.from(consumers.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsumerResponse create(@Valid @RequestBody ConsumerRequest request) {
        return ConsumerResponse.from(consumers.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public ConsumerResponse update(
            @PathVariable("id") Long id, @Valid @RequestBody ConsumerRequest request) {
        return ConsumerResponse.from(consumers.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        consumers.delete(id);
    }
}
