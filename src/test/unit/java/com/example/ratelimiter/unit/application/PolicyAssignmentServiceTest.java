package com.example.ratelimiter.unit.application;

import static com.example.ratelimiter.unit.support.TestData.onGroup;
import static com.example.ratelimiter.unit.support.TestData.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ratelimiter.application.port.in.command.PolicyAssignmentCommand;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import com.example.ratelimiter.application.service.PolicyAssignmentService;
import com.example.ratelimiter.application.service.RuleAffectingTransaction;
import com.example.ratelimiter.domain.exception.AssignmentConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.service.AssignmentConflictDetector;
import com.example.ratelimiter.unit.support.DirectUnitOfWork;
import com.example.ratelimiter.unit.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyAssignmentServiceTest {

    private static final long CONSUMER_GROUP = 2L;
    private static final long ORDERS_GROUP = 100L;

    @Mock
    private PolicyAssignmentRepositoryPort assignments;
    @Mock
    private ConsumerGroupRepositoryPort consumerGroups;
    @Mock
    private PolicyRepositoryPort policies;
    @Mock
    private ResourceRepositoryPort resources;
    @Mock
    private ResourceGroupRepositoryPort resourceGroups;
    @Mock
    private RuleCacheInvalidationPort ruleCache;

    private PolicyAssignmentService service;

    @BeforeEach
    void setUp() {
        DirectUnitOfWork unitOfWork = new DirectUnitOfWork();
        service = new PolicyAssignmentService(assignments, consumerGroups, policies, resources,
                resourceGroups, new AssignmentConflictDetector(), unitOfWork,
                new RuleAffectingTransaction(unitOfWork, ruleCache));
        when(consumerGroups.findByIdForUpdate(CONSUMER_GROUP))
                .thenReturn(Optional.of(new ConsumerGroup(CONSUMER_GROUP,
                        ConsumerGroupName.REGULAR)));
    }

    @Test
    void rejectsResourceAssignmentCoveredByAnAssignedGroup() {
        when(policies.findById(10L)).thenReturn(Optional.of(TestData.policy(10, 5)));
        when(resources.findById(11L)).thenReturn(Optional.of(
                resource(11, HttpMethod.GET, "/orders/{id}", ORDERS_GROUP)));
        when(assignments.findByConsumerGroupId(CONSUMER_GROUP))
                .thenReturn(List.of(onGroup(1, CONSUMER_GROUP, ORDERS_GROUP)));
        when(resources.findAllByIds(anyCollection())).thenReturn(List.of(
                resource(11, HttpMethod.GET, "/orders/{id}", ORDERS_GROUP)));

        assertThatThrownBy(() -> service.create(new PolicyAssignmentCommand(
                CONSUMER_GROUP, RelationshipType.RESOURCE, 10L, 11L)))
                .isInstanceOf(AssignmentConflictException.class);
        verify(assignments, never()).save(any());
        verify(ruleCache, never()).invalidateAll();
    }

    @Test
    void createsDisjointAssignmentAndInvalidatesCache() {
        when(policies.findById(10L)).thenReturn(Optional.of(TestData.policy(10, 5)));
        when(resourceGroups.findById(ORDERS_GROUP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new PolicyAssignmentCommand(
                CONSUMER_GROUP, RelationshipType.RESOURCE_GROUP, 10L, ORDERS_GROUP)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Resource group");
    }

    @Test
    void savesValidAssignment() {
        when(policies.findById(10L)).thenReturn(Optional.of(TestData.policy(10, 5)));
        when(resources.findById(11L)).thenReturn(Optional.of(
                resource(11, HttpMethod.GET, "/orders/{id}", ORDERS_GROUP)));
        when(assignments.findByConsumerGroupId(CONSUMER_GROUP)).thenReturn(List.of());
        when(resources.findAllByIds(anyCollection())).thenReturn(List.of(
                resource(11, HttpMethod.GET, "/orders/{id}", ORDERS_GROUP)));
        when(assignments.save(any(PolicyAssignment.class)))
                .thenAnswer(call -> ((PolicyAssignment) call.getArgument(0)).withId(77L));

        PolicyAssignment saved = service.create(new PolicyAssignmentCommand(
                CONSUMER_GROUP, RelationshipType.RESOURCE, 10L, 11L));

        assertThat(saved.id()).isEqualTo(77L);
        verify(ruleCache).invalidateAll();
    }
}
