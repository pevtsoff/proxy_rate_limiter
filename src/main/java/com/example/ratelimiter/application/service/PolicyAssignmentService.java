package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ManagePolicyAssignmentsUseCase;
import com.example.ratelimiter.application.port.in.command.PolicyAssignmentCommand;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.AssignmentFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.service.AssignmentConflictDetector;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements MUC1.5 and MUC2. The consumer group row is locked for the duration of the
 * transaction, so concurrent assignment changes of one group are serialised and the conflict
 * check cannot be bypassed by a race.
 */
public final class PolicyAssignmentService implements ManagePolicyAssignmentsUseCase {

    private static final String ENTITY = "Policy assignment";

    private final PolicyAssignmentRepositoryPort assignments;
    private final ConsumerGroupRepositoryPort consumerGroups;
    private final PolicyRepositoryPort policies;
    private final ResourceRepositoryPort resources;
    private final ResourceGroupRepositoryPort resourceGroups;
    private final AssignmentConflictDetector conflictDetector;
    private final UnitOfWorkPort unitOfWork;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public PolicyAssignmentService(
            PolicyAssignmentRepositoryPort assignments,
            ConsumerGroupRepositoryPort consumerGroups,
            PolicyRepositoryPort policies,
            ResourceRepositoryPort resources,
            ResourceGroupRepositoryPort resourceGroups,
            AssignmentConflictDetector conflictDetector,
            UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.assignments = assignments;
        this.consumerGroups = consumerGroups;
        this.policies = policies;
        this.resources = resources;
        this.resourceGroups = resourceGroups;
        this.conflictDetector = conflictDetector;
        this.unitOfWork = unitOfWork;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public PolicyAssignment create(PolicyAssignmentCommand command) {
        return ruleAffectingTransaction.execute(() -> validateAndSave(toAssignment(null, command)));
    }

    @Override
    public PolicyAssignment update(Long id, PolicyAssignmentCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            load(id);
            return validateAndSave(toAssignment(id, command));
        });
    }

    @Override
    public void delete(Long id) {
        ruleAffectingTransaction.run(() -> {
            load(id);
            assignments.deleteById(id);
        });
    }

    @Override
    public PolicyAssignment get(Long id) {
        return unitOfWork.inReadOnlyTransaction(() -> load(id));
    }

    @Override
    public PageResult<PolicyAssignment> list(AssignmentFilter filter, PageQuery page) {
        return unitOfWork.inReadOnlyTransaction(() -> assignments.findAll(filter, page));
    }

    private static PolicyAssignment toAssignment(Long id, PolicyAssignmentCommand command) {
        return new PolicyAssignment(id, command.consumerGroupId(), command.relationshipType(),
                command.policyId(), command.targetId());
    }

    private PolicyAssignment validateAndSave(PolicyAssignment candidate) {
        lockConsumerGroup(candidate.consumerGroupId());
        policies.findById(candidate.policyId())
                .orElseThrow(() -> new NotFoundException("Policy", candidate.policyId()));
        ensureTargetExists(candidate);
        List<PolicyAssignment> existing =
                assignments.findByConsumerGroupId(candidate.consumerGroupId());
        conflictDetector.ensureNoConflict(candidate, existing, resourceGroupsOf(candidate,
                existing));
        return assignments.save(candidate);
    }

    private void lockConsumerGroup(Long consumerGroupId) {
        consumerGroups.findByIdForUpdate(consumerGroupId)
                .orElseThrow(() -> new NotFoundException("Consumer group", consumerGroupId));
    }

    private void ensureTargetExists(PolicyAssignment candidate) {
        Long targetId = candidate.targetId();
        boolean exists = candidate.relationshipType() == RelationshipType.RESOURCE
                ? resources.findById(targetId).isPresent()
                : resourceGroups.findById(targetId).isPresent();
        if (!exists) {
            throw new NotFoundException(candidate.relationshipType() == RelationshipType.RESOURCE
                    ? "Resource" : "Resource group", targetId);
        }
    }

    private Map<Long, Long> resourceGroupsOf(
            PolicyAssignment candidate, List<PolicyAssignment> existing) {
        Set<Long> resourceIds = new HashSet<>();
        existing.stream()
                .filter(assignment -> assignment.relationshipType() == RelationshipType.RESOURCE)
                .forEach(assignment -> resourceIds.add(assignment.targetId()));
        if (candidate.relationshipType() == RelationshipType.RESOURCE) {
            resourceIds.add(candidate.targetId());
        }
        return resources.findAllByIds(resourceIds).stream()
                .filter(resource -> resource.resourceGroupId() != null)
                .collect(Collectors.toMap(Resource::id, Resource::resourceGroupId));
    }

    private PolicyAssignment load(Long id) {
        return assignments.findById(id).orElseThrow(() -> new NotFoundException(ENTITY, id));
    }
}
