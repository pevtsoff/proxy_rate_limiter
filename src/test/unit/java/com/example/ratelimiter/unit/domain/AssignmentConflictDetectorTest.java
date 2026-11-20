package com.example.ratelimiter.unit.domain;

import static com.example.ratelimiter.unit.support.TestData.onGroup;
import static com.example.ratelimiter.unit.support.TestData.onResource;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ratelimiter.domain.exception.AssignmentConflictException;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.service.AssignmentConflictDetector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssignmentConflictDetectorTest {

    private static final long CONSUMER_GROUP = 1L;
    private static final long ORDERS_GROUP = 100L;
    private static final long USERS_GROUP = 200L;
    /** Resource 11 belongs to "orders", resource 21 to "users". */
    private static final Map<Long, Long> GROUP_OF_RESOURCE = Map.of(11L, ORDERS_GROUP, 21L,
            USERS_GROUP);

    private final AssignmentConflictDetector detector = new AssignmentConflictDetector();

    @Test
    void rejectsResourceAssignmentWhenItsGroupIsAssigned() {
        PolicyAssignment candidate = newAssignment(RelationshipType.RESOURCE, 11L);
        assertThatThrownBy(() -> detector.ensureNoConflict(candidate,
                List.of(onGroup(1, CONSUMER_GROUP, ORDERS_GROUP)), GROUP_OF_RESOURCE))
                .isInstanceOf(AssignmentConflictException.class)
                .hasMessageContaining("belongs to resource group");
    }

    @Test
    void rejectsGroupAssignmentWhenOneOfItsResourcesIsAssigned() {
        PolicyAssignment candidate = newAssignment(RelationshipType.RESOURCE_GROUP, ORDERS_GROUP);
        assertThatThrownBy(() -> detector.ensureNoConflict(candidate,
                List.of(onResource(1, CONSUMER_GROUP, 11L)), GROUP_OF_RESOURCE))
                .isInstanceOf(AssignmentConflictException.class);
    }

    @Test
    void rejectsDuplicateTargets() {
        assertThatThrownBy(() -> detector.ensureNoConflict(
                newAssignment(RelationshipType.RESOURCE, 11L),
                List.of(onResource(1, CONSUMER_GROUP, 11L)), GROUP_OF_RESOURCE))
                .isInstanceOf(AssignmentConflictException.class)
                .hasMessageContaining("already has assignment");
        assertThatThrownBy(() -> detector.ensureNoConflict(
                newAssignment(RelationshipType.RESOURCE_GROUP, ORDERS_GROUP),
                List.of(onGroup(1, CONSUMER_GROUP, ORDERS_GROUP)), GROUP_OF_RESOURCE))
                .isInstanceOf(AssignmentConflictException.class);
    }

    @Test
    void acceptsDisjointAssignments() {
        List<PolicyAssignment> existing =
                List.of(onGroup(1, CONSUMER_GROUP, USERS_GROUP), onResource(2, CONSUMER_GROUP,
                        12L));
        assertThatCode(() -> detector.ensureNoConflict(
                newAssignment(RelationshipType.RESOURCE, 11L), existing, GROUP_OF_RESOURCE))
                .doesNotThrowAnyException();
        assertThatCode(() -> detector.ensureNoConflict(
                newAssignment(RelationshipType.RESOURCE_GROUP, ORDERS_GROUP), existing,
                GROUP_OF_RESOURCE))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresTheAssignmentBeingUpdated() {
        PolicyAssignment updated =
                new PolicyAssignment(7L, CONSUMER_GROUP, RelationshipType.RESOURCE, 99L, 11L);
        assertThatCode(() -> detector.ensureNoConflict(updated,
                List.of(onResource(7, CONSUMER_GROUP, 11L)), GROUP_OF_RESOURCE))
                .doesNotThrowAnyException();
    }

    private static PolicyAssignment newAssignment(RelationshipType type, long targetId) {
        return PolicyAssignment.newAssignment(CONSUMER_GROUP, type, 10L, targetId);
    }
}
