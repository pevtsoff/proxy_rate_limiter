package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ManageConsumersUseCase;
import com.example.ratelimiter.application.port.in.command.ConsumerCommand;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;

/**
 * Implements MUC1.2. Group rows are locked before the "is this the last consumer?" check so
 * that two concurrent deletions cannot both pass it and leave the group empty.
 */
public final class ConsumerService implements ManageConsumersUseCase {

    private static final String ENTITY = "Consumer";
    private static final String GROUP_ENTITY = "Consumer group";

    private final ConsumerRepositoryPort consumers;
    private final ConsumerGroupRepositoryPort groups;
    private final UnitOfWorkPort unitOfWork;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    public ConsumerService(
            ConsumerRepositoryPort consumers,
            ConsumerGroupRepositoryPort groups,
            UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.consumers = consumers;
        this.groups = groups;
        this.unitOfWork = unitOfWork;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public Consumer create(ConsumerCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            lockGroup(command.consumerGroupId());
            ensureConsumerIdAvailable(command.consumerId(), null);
            return consumers.save(
                    Consumer.newConsumer(command.consumerId(), command.consumerGroupId()));
        });
    }

    @Override
    public Consumer update(Long id, ConsumerCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            Consumer existing = load(id);
            lockGroupsInIdOrder(existing.consumerGroupId(), command.consumerGroupId());
            ensureConsumerIdAvailable(command.consumerId(), id);
            if (!existing.consumerGroupId().equals(command.consumerGroupId())) {
                ensureNotLastConsumer(existing);
            }
            return consumers.save(
                    existing.withDetails(command.consumerId(), command.consumerGroupId()));
        });
    }

    @Override
    public void delete(Long id) {
        ruleAffectingTransaction.run(() -> {
            Consumer existing = load(id);
            lockGroup(existing.consumerGroupId());
            ensureNotLastConsumer(existing);
            consumers.deleteById(id);
        });
    }

    @Override
    public Consumer get(Long id) {
        return unitOfWork.inReadOnlyTransaction(() -> load(id));
    }

    @Override
    public PageResult<Consumer> list(ConsumerFilter filter, PageQuery page) {
        return unitOfWork.inReadOnlyTransaction(() -> consumers.findAll(filter, page));
    }

    private Consumer load(Long id) {
        return consumers.findById(id).orElseThrow(() -> new NotFoundException(ENTITY, id));
    }

    private void lockGroup(Long groupId) {
        groups.findByIdForUpdate(groupId)
                .orElseThrow(() -> new NotFoundException(GROUP_ENTITY, groupId));
    }

    /** Always locking the lower id first prevents deadlocks between opposite moves. */
    private void lockGroupsInIdOrder(Long first, Long second) {
        lockGroup(Math.min(first, second));
        if (!first.equals(second)) {
            lockGroup(Math.max(first, second));
        }
    }

    private void ensureConsumerIdAvailable(String consumerId, Long ownId) {
        consumers.findByConsumerId(consumerId == null ? "" : consumerId.trim())
                .filter(other -> !other.id().equals(ownId))
                .ifPresent(other -> {
                    throw new ConflictException("Consumer '" + consumerId + "' already exists");
                });
    }

    private void ensureNotLastConsumer(Consumer consumer) {
        if (consumers.countByConsumerGroupId(consumer.consumerGroupId()) <= 1) {
            throw new ConflictException("Consumer '" + consumer.consumerId()
                    + "' is the last consumer of its group; a consumer group must have at least "
                    + "one consumer (delete the group instead)");
        }
    }
}
