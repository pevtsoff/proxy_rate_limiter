package com.example.ratelimiter.domain.model;

/** Optional criteria for listing policy assignments; {@code null} fields are ignored. */
public record AssignmentFilter(
        Long consumerGroupId, RelationshipType relationshipType, Long policyId) {

    public static AssignmentFilter none() {
        return new AssignmentFilter(null, null, null);
    }
}
