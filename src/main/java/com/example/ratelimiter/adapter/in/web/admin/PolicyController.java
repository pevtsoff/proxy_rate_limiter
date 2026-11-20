package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.PageResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.PolicyRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.PolicyResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.StrategiesResponse;
import com.example.ratelimiter.application.port.in.ManagePoliciesUseCase;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PolicyFilter;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
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

/** MUC3 / MUC4: CRUD for rate-limiting policies. */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/policies")
public class PolicyController {

    private final ManagePoliciesUseCase policies;

    public PolicyController(ManagePoliciesUseCase policies) {
        this.policies = policies;
    }

    @GetMapping
    public PageResponse<PolicyResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String strategy,
            @RequestParam(defaultValue = AdminApi.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AdminApi.DEFAULT_SIZE) int size) {
        PolicyFilter filter = new PolicyFilter(
                name, QueryParams.optional(strategy, RateLimitingStrategy::fromValue));
        return PageResponse.from(
                policies.list(filter, new PageQuery(page, size)), PolicyResponse::from);
    }

    @GetMapping("/strategies")
    public StrategiesResponse strategies() {
        return new StrategiesResponse(policies.supportedStrategies(),
                RateLimitingStrategy.DEFAULT, List.of(TimePeriod.values()));
    }

    @GetMapping("/default")
    public PolicyResponse defaultPolicy() {
        return PolicyResponse.from(policies.getDefault());
    }

    @GetMapping("/{id}")
    public PolicyResponse get(@PathVariable("id") Long id) {
        return PolicyResponse.from(policies.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyResponse create(@Valid @RequestBody PolicyRequest request) {
        return PolicyResponse.from(policies.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public PolicyResponse update(@PathVariable("id") Long id,
            @Valid @RequestBody PolicyRequest request) {
        return PolicyResponse.from(policies.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        policies.delete(id);
    }
}
