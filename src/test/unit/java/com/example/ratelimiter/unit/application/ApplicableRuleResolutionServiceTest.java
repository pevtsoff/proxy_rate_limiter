package com.example.ratelimiter.unit.application;

import static com.example.ratelimiter.unit.support.TestData.onGroup;
import static com.example.ratelimiter.unit.support.TestData.onResource;
import static com.example.ratelimiter.unit.support.TestData.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.service.ApplicableRuleResolutionService;
import com.example.ratelimiter.domain.exception.UnknownConsumerException;
import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.service.AssignmentSelector;
import com.example.ratelimiter.domain.service.ResourcePathMatcher;
import com.example.ratelimiter.unit.support.DirectUnitOfWork;
import com.example.ratelimiter.unit.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CUC1.3: consumer -> group -> resource -> assignment -> policy, with default fallback. */
@ExtendWith(MockitoExtension.class)
class ApplicableRuleResolutionServiceTest {

    private static final Resource ORDER_BY_ID = resource(11, HttpMethod.GET, "/orders/{id}", 100L);

    @Mock
    private ConsumerRepositoryPort consumers;
    @Mock
    private ResourceRepositoryPort resources;
    @Mock
    private PolicyAssignmentRepositoryPort assignments;
    @Mock
    private PolicyRepositoryPort policies;

    private ApplicableRuleResolutionService service;

    @BeforeEach
    void setUp() {
        service = new ApplicableRuleResolutionService(consumers, resources, assignments, policies,
                new ResourcePathMatcher(), new AssignmentSelector(), new DirectUnitOfWork());
    }

    @Test
    void resolvesResourceLevelPolicy() {
        givenConsumerAndCatalogue();
        when(assignments.findByConsumerGroupId(2L)).thenReturn(List.of(onResource(1, 2, 11L)));
        when(policies.findById(10L)).thenReturn(Optional.of(TestData.policy(10, 5)));

        ApplicableRule rule = service.resolve("alice", HttpMethod.GET, "/orders/42");

        assertThat(rule.scope()).isEqualTo("resource:11");
        assertThat(rule.policy().numberOfRequests()).isEqualTo(5);
    }

    @Test
    void resolvesResourceGroupPolicySharedByAllResourcesOfTheGroup() {
        givenConsumerAndCatalogue();
        when(assignments.findByConsumerGroupId(2L)).thenReturn(List.of(onGroup(1, 2, 100L)));
        when(policies.findById(10L)).thenReturn(Optional.of(TestData.policy(10, 5)));

        assertThat(service.resolve("alice", HttpMethod.GET, "/orders/42").scope())
                .isEqualTo("resource-group:100");
    }

    @Test
    void fallsBackToDefaultPolicyWhenNothingIsAssigned() {
        givenConsumerAndCatalogue();
        when(assignments.findByConsumerGroupId(2L)).thenReturn(List.of());
        when(policies.findDefault()).thenReturn(Optional.of(TestData.defaultPolicy()));

        ApplicableRule rule = service.resolve("alice", HttpMethod.GET, "/orders/42");

        assertThat(rule.policy().defaultPolicy()).isTrue();
        assertThat(rule.scope()).isEqualTo("default:resource:11");
    }

    @Test
    void unknownPathsShareOneDefaultBudget() {
        givenConsumerAndCatalogue();
        when(policies.findDefault()).thenReturn(Optional.of(TestData.defaultPolicy()));

        assertThat(service.resolve("alice", HttpMethod.GET, "/payments/1").scope())
                .isEqualTo("default:unmatched");
    }

    @Test
    void unknownConsumerIsRejected() {
        when(consumers.findByConsumerId("mallory")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve("mallory", HttpMethod.GET, "/orders"))
                .isInstanceOf(UnknownConsumerException.class);
    }

    private void givenConsumerAndCatalogue() {
        when(consumers.findByConsumerId("alice"))
                .thenReturn(Optional.of(new Consumer(1L, "alice", 2L)));
        when(resources.findAll()).thenReturn(List.of(ORDER_BY_ID));
    }
}
