package com.example.ratelimiter.domain.service;

import com.example.ratelimiter.domain.exception.AssignmentConflictException;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import java.util.Collection;
import java.util.Map;

/**
 * Enforces the {@code consumer_groups_policies} invariants for one consumer group.
 *
 * <ul>
 *   <li>a resource or resource group is assigned at most once;</li>
 *   <li>a resource cannot be assigned directly while its resource group is assigned, and a
 *       resource group cannot be assigned while one of its resources is assigned directly.</li>
 * </ul>
 */
public final class AssignmentConflictDetector {

    /**
     * Verifies that {@code candidate} can coexist with the group's other assignments.
     *
     * @param existing                  current assignments of the candidate's consumer group;
     *                                  the candidate itself (on update) is ignored
     * @param resourceGroupByResourceId resource group of every resource referenced by the
     *                                  candidate or by the existing resource-level assignments
     * @throws AssignmentConflictException when the candidate overlaps an existing assignment
     */
    public void ensureNoConflict(
            PolicyAssignment candidate,
            Collection<PolicyAssignment> existing,
            Map<Long, Long> resourceGroupByResourceId) {
        for (PolicyAssignment other : existing) {
            if (!other.isSameAssignmentAs(candidate)) {
                ensureCompatible(candidate, other, resourceGroupByResourceId);
            }
        }
    }

    private static void ensureCompatible(
            PolicyAssignment candidate, PolicyAssignment other, Map<Long, Long> groupOfResource) {
        if (candidate.relationshipType() == other.relationshipType()) {
            ensureDifferentTarget(candidate, other);
        } else if (candidate.relationshipType() == RelationshipType.RESOURCE) {
            ensureResourceOutsideGroup(candidate.targetId(), other.targetId(), groupOfResource);
        } else {
            ensureResourceOutsideGroup(other.targetId(), candidate.targetId(), groupOfResource);
        }
    }

    private static void ensureDifferentTarget(PolicyAssignment candidate, PolicyAssignment other) {
        if (candidate.targetId().equals(other.targetId())) {
            throw new AssignmentConflictException(String.format(
                    "Consumer group %d already has assignment %d for %s %d",
                    candidate.consumerGroupId(), other.id(),
                    candidate.relationshipType().value(), candidate.targetId()));
        }
    }

    private static void ensureResourceOutsideGroup(
            Long resourceId, Long resourceGroupId, Map<Long, Long> groupOfResource) {
        if (resourceGroupId.equals(groupOfResource.get(resourceId))) {
            throw new AssignmentConflictException(String.format(
                    "Resource %d belongs to resource group %d; the same consumer group cannot "
                            + "have assignments for both", resourceId, resourceGroupId));
        }
    }
}
