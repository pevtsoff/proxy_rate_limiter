package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.PageResponse;
import com.example.ratelimiter.adapter.in.web.admin.dto.PolicyAssignmentRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.PolicyAssignmentResponse;
import com.example.ratelimiter.application.port.in.ManagePolicyAssignmentsUseCase;
import com.example.ratelimiter.domain.model.AssignmentFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.RelationshipType;
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

/** MUC1.5 / MUC2: CRUD over {@code consumer_groups_policies}. */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/consumer-groups-policies")
public class PolicyAssignmentController {

    private final ManagePolicyAssignmentsUseCase assignments;

    public PolicyAssignmentController(ManagePolicyAssignmentsUseCase assignments) {
        this.assignments = assignments;
    }

    @GetMapping
    public PageResponse<PolicyAssignmentResponse> list(
            @RequestParam(name = "consumer_group_id", required = false) Long consumerGroupId,
            @RequestParam(name = "relationship_type", required = false) String relationshipType,
            @RequestParam(name = "policy_id", required = false) Long policyId,
            @RequestParam(defaultValue = AdminApi.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AdminApi.DEFAULT_SIZE) int size) {
        AssignmentFilter filter = new AssignmentFilter(consumerGroupId,
                QueryParams.optional(relationshipType, RelationshipType::fromValue), policyId);
        return PageResponse.from(
                assignments.list(filter, new PageQuery(page, size)),
                        PolicyAssignmentResponse::from);
    }

    @GetMapping("/{id}")
    public PolicyAssignmentResponse get(@PathVariable("id") Long id) {
        return PolicyAssignmentResponse.from(assignments.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyAssignmentResponse create(@Valid @RequestBody PolicyAssignmentRequest request) {
        return PolicyAssignmentResponse.from(assignments.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public PolicyAssignmentResponse update(
            @PathVariable("id") Long id, @Valid @RequestBody PolicyAssignmentRequest request) {
        return PolicyAssignmentResponse.from(assignments.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        assignments.delete(id);
    }
}
