package com.example.ratelimiter.unit.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ratelimiter.application.port.in.command.ConsumerCommand;
import com.example.ratelimiter.application.port.in.command.CreateConsumerGroupCommand;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import com.example.ratelimiter.application.service.ConsumerGroupService;
import com.example.ratelimiter.application.service.ConsumerService;
import com.example.ratelimiter.application.service.RuleAffectingTransaction;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.exception.ValidationException;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.example.ratelimiter.unit.support.DirectUnitOfWork;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Business rules around consumers and consumer groups (MUC1). */
@ExtendWith(MockitoExtension.class)
class ConsumerManagementTest {

    private static final ConsumerGroup FREE = new ConsumerGroup(3L, ConsumerGroupName.FREE);

    @Mock
    private ConsumerGroupRepositoryPort groups;
    @Mock
    private ConsumerRepositoryPort consumers;
    @Mock
    private PolicyAssignmentRepositoryPort assignments;
    @Mock
    private RuleCacheInvalidationPort ruleCache;

    private ConsumerGroupService groupService;
    private ConsumerService consumerService;

    @BeforeEach
    void setUp() {
        DirectUnitOfWork unitOfWork = new DirectUnitOfWork();
        RuleAffectingTransaction transaction = new RuleAffectingTransaction(unitOfWork, ruleCache);
        groupService = new ConsumerGroupService(groups, consumers, assignments, unitOfWork,
                transaction);
        consumerService = new ConsumerService(consumers, groups, unitOfWork, transaction);
    }

    @Test
    void groupCannotBeCreatedWithoutConsumers() {
        assertThatThrownBy(() -> groupService.create(
                new CreateConsumerGroupCommand(ConsumerGroupName.FREE, List.of())))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void groupIsCreatedTogetherWithItsConsumers() {
        when(groups.findByName(ConsumerGroupName.FREE)).thenReturn(Optional.empty());
        when(consumers.findByConsumerId(any())).thenReturn(Optional.empty());
        when(groups.save(any(ConsumerGroup.class))).thenReturn(FREE);

        groupService.create(new CreateConsumerGroupCommand(ConsumerGroupName.FREE,
                List.of("alice", "bob")));

        verify(consumers, times(2)).save(any(Consumer.class));
        verify(ruleCache).invalidateAll();
    }

    @Test
    void groupNamesAreUnique() {
        when(groups.findByName(ConsumerGroupName.FREE)).thenReturn(Optional.of(FREE));
        assertThatThrownBy(() -> groupService.create(
                new CreateConsumerGroupCommand(ConsumerGroupName.FREE, List.of("alice"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void consumerMustReferenceAnExistingGroup() {
        when(groups.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> consumerService.create(new ConsumerCommand("alice", 99L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void lastConsumerOfAGroupCannotBeDeleted() {
        when(consumers.findById(5L)).thenReturn(Optional.of(new Consumer(5L, "alice", 3L)));
        when(groups.findByIdForUpdate(3L)).thenReturn(Optional.of(FREE));
        when(consumers.countByConsumerGroupId(3L)).thenReturn(1L);

        assertThatThrownBy(() -> consumerService.delete(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("at least one consumer");
        verify(consumers, never()).deleteById(5L);
    }

    @Test
    void lastConsumerCannotBeMovedToAnotherGroup() {
        when(consumers.findById(5L)).thenReturn(Optional.of(new Consumer(5L, "alice", 3L)));
        when(groups.findByIdForUpdate(any()))
                .thenReturn(Optional.of(new ConsumerGroup(4L, ConsumerGroupName.PREMIUM)));
        when(consumers.findByConsumerId("alice"))
                .thenReturn(Optional.of(new Consumer(5L, "alice", 3L)));
        when(consumers.countByConsumerGroupId(3L)).thenReturn(1L);

        assertThatThrownBy(() -> consumerService.update(5L, new ConsumerCommand("alice", 4L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deletingAGroupRemovesItsConsumersAndAssignments() {
        when(groups.findByIdForUpdate(3L)).thenReturn(Optional.of(FREE));

        groupService.delete(3L);

        verify(assignments).deleteByConsumerGroupId(3L);
        verify(consumers).deleteByConsumerGroupId(3L);
        verify(groups).deleteById(3L);
    }
}
