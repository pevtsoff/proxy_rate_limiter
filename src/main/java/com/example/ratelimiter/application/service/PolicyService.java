package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ManagePoliciesUseCase;
import com.example.ratelimiter.application.port.in.command.PolicyCommand;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyFilter;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import java.util.List;

/** Implements MUC3 / MUC4. */
public final class PolicyService implements ManagePoliciesUseCase {

    private static final String ENTITY = "Policy";

    private final PolicyRepositoryPort policies;
    private final PolicyAssignmentRepositoryPort assignments;
    private final UnitOfWorkPort unitOfWork;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    public PolicyService(
            PolicyRepositoryPort policies,
            PolicyAssignmentRepositoryPort assignments,
            UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.policies = policies;
        this.assignments = assignments;
        this.unitOfWork = unitOfWork;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public Policy create(PolicyCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            ensureNameAvailable(command.name(), null);
            return policies.save(Policy.newPolicy(command.name(), command.strategy(),
                    command.numberOfRequests(), command.timePeriod()));
        });
    }

    @Override
    public Policy update(Long id, PolicyCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            Policy existing = load(id);
            ensureNameAvailable(command.name(), id);
            return policies.save(existing.withDefinition(command.name(), command.strategy(),
                    command.numberOfRequests(), command.timePeriod()));
        });
    }

    @Override
    public void delete(Long id) {
        ruleAffectingTransaction.run(() -> {
            ensureDeletable(load(id));
            policies.deleteById(id);
        });
    }

    @Override
    public Policy get(Long id) {
        return unitOfWork.inReadOnlyTransaction(() -> load(id));
    }

    @Override
    public PageResult<Policy> list(PolicyFilter filter, PageQuery page) {
        return unitOfWork.inReadOnlyTransaction(() -> policies.findAll(filter, page));
    }

    @Override
    public Policy getDefault() {
        return unitOfWork.inReadOnlyTransaction(() -> policies.findDefault()
                .orElseThrow(() -> new NotFoundException(ENTITY, "default")));
    }

    @Override
    public List<RateLimitingStrategy> supportedStrategies() {
        return List.of(RateLimitingStrategy.values());
    }

    private Policy load(Long id) {
        return policies.findById(id).orElseThrow(() -> new NotFoundException(ENTITY, id));
    }

    private void ensureNameAvailable(String name, Long ownId) {
        policies.findByName(name)
                .filter(other -> !other.id().equals(ownId))
                .ifPresent(other -> {
                    throw new ConflictException("Policy name '" + name + "' is already used");
                });
    }

    private void ensureDeletable(Policy policy) {
        if (policy.defaultPolicy()) {
            throw new ConflictException("The default policy cannot be deleted; update it instead");
        }
        if (assignments.existsByPolicyId(policy.id())) {
            throw new ConflictException(
                    "Policy " + policy.id() + " is still assigned; delete its assignments first");
        }
    }
}
