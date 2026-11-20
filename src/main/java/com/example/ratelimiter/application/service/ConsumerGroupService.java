package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ManageConsumerGroupsUseCase;
import com.example.ratelimiter.application.port.in.command.CreateConsumerGroupCommand;
import com.example.ratelimiter.application.port.in.command.UpdateConsumerGroupCommand;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.exception.ValidationException;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import java.util.HashSet;
import java.util.List;

/** Implements MUC1.1: consumer groups are created together with at least one consumer. */
public final class ConsumerGroupService implements ManageConsumerGroupsUseCase {

    private static final String ENTITY = "Consumer group";

    private final ConsumerGroupRepositoryPort groups;
    private final ConsumerRepositoryPort consumers;
    private final PolicyAssignmentRepositoryPort assignments;
    private final UnitOfWorkPort unitOfWork;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    public ConsumerGroupService(
            ConsumerGroupRepositoryPort groups,
            ConsumerRepositoryPort consumers,
            PolicyAssignmentRepositoryPort assignments,
            UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.groups = groups;
        this.consumers = consumers;
        this.assignments = assignments;
        this.unitOfWork = unitOfWork;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public ConsumerGroup create(CreateConsumerGroupCommand command) {
        ensureValidInitialConsumers(command.initialConsumerIds());
        return ruleAffectingTransaction.execute(() -> {
            ensureNameAvailable(command.name(), null);
            command.initialConsumerIds().forEach(this::ensureConsumerIdAvailable);
            ConsumerGroup group = groups.save(ConsumerGroup.newGroup(command.name()));
            command.initialConsumerIds()
                    .forEach(id -> consumers.save(Consumer.newConsumer(id, group.id())));
            return group;
        });
    }

    @Override
    public ConsumerGroup update(Long id, UpdateConsumerGroupCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            lock(id);
            ensureNameAvailable(command.name(), id);
            return groups.save(new ConsumerGroup(id, command.name()));
        });
    }

    @Override
    public void delete(Long id) {
        ruleAffectingTransaction.run(() -> {
            lock(id);
            assignments.deleteByConsumerGroupId(id);
            consumers.deleteByConsumerGroupId(id);
            groups.deleteById(id);
        });
    }

    @Override
    public ConsumerGroup get(Long id) {
        return unitOfWork.inReadOnlyTransaction(() -> groups.findById(id)
                .orElseThrow(() -> new NotFoundException(ENTITY, id)));
    }

    @Override
    public PageResult<ConsumerGroup> list(PageQuery page) {
        return unitOfWork.inReadOnlyTransaction(() -> groups.findAll(page));
    }

    private void lock(Long id) {
        groups.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException(ENTITY, id));
    }

    private static void ensureValidInitialConsumers(List<String> consumerIds) {
        if (consumerIds.isEmpty()) {
            throw new ValidationException("A consumer group must have at least one consumer");
        }
        if (new HashSet<>(consumerIds).size() != consumerIds.size()) {
            throw new ValidationException("Initial consumer ids must be unique");
        }
    }

    private void ensureNameAvailable(ConsumerGroupName name, Long ownId) {
        groups.findByName(name)
                .filter(other -> !other.id().equals(ownId))
                .ifPresent(other -> {
                    throw new ConflictException(
                            "Consumer group '" + name.value() + "' already exists");
                });
    }

    private void ensureConsumerIdAvailable(String consumerId) {
        consumers.findByConsumerId(consumerId).ifPresent(existing -> {
            throw new ConflictException("Consumer '" + consumerId + "' already exists");
        });
    }
}
