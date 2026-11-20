package com.example.ratelimiter.domain.service;

import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.Resource;
import java.util.Collection;
import java.util.Optional;

/**
 * Picks the assignment of a consumer group that governs a resource: a resource-level assignment
 * wins over a resource-group-level one. Because overlapping assignments are rejected on write
 * (see {@link AssignmentConflictDetector}) at most one of each kind can exist.
 */
public final class AssignmentSelector {

    /** Returns the assignment governing the resource, or empty when the default applies. */
    public Optional<PolicyAssignment> select(
            Resource resource, Collection<PolicyAssignment> groupAssignments) {
        Optional<PolicyAssignment> direct = groupAssignments.stream()
                .filter(assignment -> assignment.targetsResource(resource.id()))
                .findFirst();
        return direct.isPresent() ? direct : selectByResourceGroup(resource, groupAssignments);
    }

    private static Optional<PolicyAssignment> selectByResourceGroup(
            Resource resource, Collection<PolicyAssignment> groupAssignments) {
        if (resource.resourceGroupId() == null) {
            return Optional.empty();
        }
        return groupAssignments.stream()
                .filter(assignment -> assignment.targetsResourceGroup(resource.resourceGroupId()))
                .findFirst();
    }
}
