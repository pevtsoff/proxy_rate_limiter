package com.example.ratelimiter.application.port.in.command;

import com.example.ratelimiter.domain.model.RelationshipType;

/**
 * Data needed to create or update a policy assignment.
 *
 * @param targetId resource id or resource group id, depending on {@code relationshipType}
 */
public record PolicyAssignmentCommand(
        Long consumerGroupId, RelationshipType relationshipType, Long policyId, Long targetId) {
}
