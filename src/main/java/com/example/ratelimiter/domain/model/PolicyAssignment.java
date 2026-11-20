package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/**
 * Row of {@code consumer_groups_policies}: binds a policy to a consumer group for either one
 * resource or one resource group.
 *
 * @param targetId id of a resource when {@code relationshipType} is {@code resource}, id of a
 *                 resource group when it is {@code resource_group}
 */
public record PolicyAssignment(
        Long id,
        Long consumerGroupId,
        RelationshipType relationshipType,
        Long policyId,
        Long targetId) {

    public PolicyAssignment {
        requirePresent(consumerGroupId, "consumer_group_id");
        requirePresent(relationshipType, "relationship_type");
        requirePresent(policyId, "policy_id");
        requirePresent(targetId, "resource_id");
    }

    public static PolicyAssignment newAssignment(
            Long consumerGroupId, RelationshipType type, Long policyId, Long targetId) {
        return new PolicyAssignment(null, consumerGroupId, type, policyId, targetId);
    }

    public PolicyAssignment withId(Long newId) {
        return new PolicyAssignment(newId, consumerGroupId, relationshipType, policyId, targetId);
    }

    public boolean targetsResource(Long resourceId) {
        return relationshipType == RelationshipType.RESOURCE && targetId.equals(resourceId);
    }

    public boolean targetsResourceGroup(Long resourceGroupId) {
        return relationshipType == RelationshipType.RESOURCE_GROUP
                && targetId.equals(resourceGroupId);
    }

    public boolean isSameAssignmentAs(PolicyAssignment other) {
        return id != null && id.equals(other.id());
    }

    private static void requirePresent(Object value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
    }
}
