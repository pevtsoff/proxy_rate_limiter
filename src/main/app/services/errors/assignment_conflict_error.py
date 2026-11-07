"""Raised when an assignment violates consumer-group scope invariants."""


class AssignmentConflictError(ValueError):
    """A consumer group assignment overlaps an existing assignment scope."""
