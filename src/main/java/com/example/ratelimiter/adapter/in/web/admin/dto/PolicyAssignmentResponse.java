package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;

/** Policy assignment representation. */
public record PolicyAssignmentResponse(
        Long id,
        Long consumerGroupId,
        RelationshipType relationshipType,
        Long policyId,
        Long resourceId) {

    public static PolicyAssignmentResponse from(PolicyAssignment assignment) {
        return new PolicyAssignmentResponse(assignment.id(), assignment.consumerGroupId(),
                assignment.relationshipType(), assignment.policyId(), assignment.targetId());
    }
}
