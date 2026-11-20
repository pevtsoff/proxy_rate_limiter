package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.application.port.in.command.PolicyAssignmentCommand;
import com.example.ratelimiter.domain.model.RelationshipType;
import jakarta.validation.constraints.NotNull;

/**
 * Create / update payload of a {@code consumer_groups_policies} row.
 *
 * @param resourceId id of a resource or of a resource group, depending on
 *                   {@code relationshipType}
 */
public record PolicyAssignmentRequest(
        @NotNull Long consumerGroupId,
        @NotNull RelationshipType relationshipType,
        @NotNull Long policyId,
        @NotNull Long resourceId) {

    public PolicyAssignmentCommand toCommand() {
        return new PolicyAssignmentCommand(consumerGroupId, relationshipType, policyId, resourceId);
    }
}
