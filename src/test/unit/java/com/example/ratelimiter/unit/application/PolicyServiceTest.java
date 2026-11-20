package com.example.ratelimiter.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ratelimiter.application.port.in.command.PolicyCommand;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import com.example.ratelimiter.application.service.PolicyService;
import com.example.ratelimiter.application.service.RuleAffectingTransaction;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import com.example.ratelimiter.unit.support.DirectUnitOfWork;
import com.example.ratelimiter.unit.support.TestData;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepositoryPort policies;
    @Mock
    private PolicyAssignmentRepositoryPort assignments;
    @Mock
    private RuleCacheInvalidationPort ruleCache;

    private PolicyService service;

    @BeforeEach
    void setUp() {
        DirectUnitOfWork unitOfWork = new DirectUnitOfWork();
        service = new PolicyService(policies, assignments, unitOfWork,
                new RuleAffectingTransaction(unitOfWork, ruleCache));
    }

    @Test
    void createPersistsPolicyAndInvalidatesRuleCache() {
        when(policies.findByName("burst")).thenReturn(Optional.empty());
        when(policies.save(any(Policy.class))).thenAnswer(call -> call.getArgument(0));

        Policy created = service.create(
                new PolicyCommand("burst", null, 5, TimePeriod.SECOND));

        assertThat(created.strategy()).isEqualTo(RateLimitingStrategy.MOVING_WINDOW);
        assertThat(created.defaultPolicy()).isFalse();
        verify(ruleCache).invalidateAll();
    }

    @Test
    void createRejectsDuplicateName() {
        when(policies.findByName("burst")).thenReturn(Optional.of(TestData.policy(3, 1)));

        assertThatThrownBy(() -> service.create(
                new PolicyCommand("burst", null, 5, TimePeriod.SECOND)))
                .isInstanceOf(ConflictException.class);
        verify(ruleCache, never()).invalidateAll();
    }

    @Test
    void defaultPolicyCannotBeDeleted() {
        when(policies.findById(1L)).thenReturn(Optional.of(TestData.defaultPolicy()));

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("default");
        verify(policies, never()).deleteById(1L);
    }

    @Test
    void assignedPolicyCannotBeDeleted() {
        when(policies.findById(3L)).thenReturn(Optional.of(TestData.policy(3, 1)));
        when(assignments.existsByPolicyId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void updateKeepsIdentityAndDefaultFlag() {
        when(policies.findById(1L)).thenReturn(Optional.of(TestData.defaultPolicy()));
        when(policies.findByName("default")).thenReturn(Optional.of(TestData.defaultPolicy()));
        when(policies.save(any(Policy.class))).thenAnswer(call -> call.getArgument(0));

        Policy updated = service.update(1L, new PolicyCommand(
                "default", RateLimitingStrategy.TOKEN_BUCKET, 120, TimePeriod.HOUR));

        assertThat(updated.id()).isEqualTo(1L);
        assertThat(updated.defaultPolicy()).isTrue();
        assertThat(updated.numberOfRequests()).isEqualTo(120);
        verify(ruleCache).invalidateAll();
    }

    @Test
    void unknownPolicyIsNotFound() {
        when(policies.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(NotFoundException.class);
    }
}
