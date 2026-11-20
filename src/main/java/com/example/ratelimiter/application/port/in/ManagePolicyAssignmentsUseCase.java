package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.application.port.in.command.PolicyAssignmentCommand;
import com.example.ratelimiter.domain.model.AssignmentFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.PolicyAssignment;

/** MUC1.5 / MUC2: assign policies to consumer groups per resource or per resource group. */
public interface ManagePolicyAssignmentsUseCase {

    PolicyAssignment create(PolicyAssignmentCommand command);

    PolicyAssignment update(Long id, PolicyAssignmentCommand command);

    void delete(Long id);

    PolicyAssignment get(Long id);

    PageResult<PolicyAssignment> list(AssignmentFilter filter, PageQuery page);
}
