package com.example.ratelimiter.domain.exception;

/**
 * A policy assignment overlaps an existing assignment of the same consumer group, e.g. a
 * resource-level assignment for a resource whose resource group is already assigned.
 */
public class AssignmentConflictException extends ConflictException {

    public AssignmentConflictException(String message) {
        super(message);
    }
}
