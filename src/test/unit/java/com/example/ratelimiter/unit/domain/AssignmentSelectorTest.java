package com.example.ratelimiter.unit.domain;

import static com.example.ratelimiter.unit.support.TestData.onGroup;
import static com.example.ratelimiter.unit.support.TestData.onResource;
import static com.example.ratelimiter.unit.support.TestData.resource;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.service.AssignmentSelector;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssignmentSelectorTest {

    private final AssignmentSelector selector = new AssignmentSelector();
    private final Resource ordersById = resource(11, HttpMethod.GET, "/orders/{id}", 100L);

    @Test
    void prefersResourceLevelAssignment() {
        List<PolicyAssignment> assignments = List.of(onGroup(1, 1, 100L), onResource(2, 1, 11L));
        assertThat(selector.select(ordersById, assignments)).map(PolicyAssignment::id).contains(2L);
    }

    @Test
    void fallsBackToResourceGroupAssignment() {
        assertThat(selector.select(ordersById, List.of(onGroup(1, 1, 100L))))
                .map(PolicyAssignment::id).contains(1L);
    }

    @Test
    void returnsEmptyWhenNothingIsAssigned() {
        assertThat(selector.select(ordersById, List.of(onGroup(1, 1, 200L)))).isEmpty();
        assertThat(selector.select(resource(12, HttpMethod.GET, "/x", null),
                List.of(onGroup(1, 1, 100L)))).isEmpty();
    }
}
