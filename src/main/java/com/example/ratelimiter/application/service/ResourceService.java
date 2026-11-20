package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ManageResourcesUseCase;
import com.example.ratelimiter.application.port.in.command.CreateResourceCommand;
import com.example.ratelimiter.application.port.in.command.UpdateResourceCommand;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.AssignmentConflictException;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceFilter;
import java.util.List;
import java.util.Objects;

/**
 * CRUD for target resources. Moving a resource into another resource group is a potential
 * assignment conflict and is checked like a new assignment would be.
 */
public final class ResourceService implements ManageResourcesUseCase {

    private static final String ENTITY = "Resource";

    private final ResourceRepositoryPort resources;
    private final ResourceGroupRepositoryPort resourceGroups;
    private final PolicyAssignmentRepositoryPort assignments;
    private final UnitOfWorkPort unitOfWork;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    public ResourceService(
            ResourceRepositoryPort resources,
            ResourceGroupRepositoryPort resourceGroups,
            PolicyAssignmentRepositoryPort assignments,
            UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.resources = resources;
        this.resourceGroups = resourceGroups;
        this.assignments = assignments;
        this.unitOfWork = unitOfWork;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public Resource create(CreateResourceCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            Resource candidate = new Resource(null, command.method(), command.path(),
                    command.ioType(), command.resourceGroupId());
            ensureResourceGroupExists(candidate.resourceGroupId());
            resources.findByMethodAndPath(candidate.method(), candidate.path())
                    .ifPresent(existing -> {
                        throw new ConflictException(existing.label() + " already exists");
                    });
            return resources.save(candidate);
        });
    }

    @Override
    public Resource update(Long id, UpdateResourceCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            Resource existing = load(id);
            ensureResourceGroupExists(command.resourceGroupId());
            if (!Objects.equals(existing.resourceGroupId(), command.resourceGroupId())) {
                ensureRegroupingKeepsAssignmentsDisjoint(existing, command.resourceGroupId());
            }
            return resources.save(
                    existing.withClassification(command.ioType(), command.resourceGroupId()));
        });
    }

    @Override
    public void delete(Long id) {
        ruleAffectingTransaction.run(() -> {
            load(id);
            if (!assignments.findByTarget(RelationshipType.RESOURCE, id).isEmpty()) {
                throw new ConflictException(
                        "Resource " + id + " is still assigned; delete its assignments first");
            }
            resources.deleteById(id);
        });
    }

    @Override
    public Resource get(Long id) {
        return unitOfWork.inReadOnlyTransaction(() -> load(id));
    }

    @Override
    public PageResult<Resource> list(ResourceFilter filter, PageQuery page) {
        return unitOfWork.inReadOnlyTransaction(() -> resources.findAll(filter, page));
    }

    private Resource load(Long id) {
        return resources.findById(id).orElseThrow(() -> new NotFoundException(ENTITY, id));
    }

    private void ensureResourceGroupExists(Long resourceGroupId) {
        if (resourceGroupId != null && resourceGroups.findById(resourceGroupId).isEmpty()) {
            throw new NotFoundException("Resource group", resourceGroupId);
        }
    }

    private void ensureRegroupingKeepsAssignmentsDisjoint(Resource resource, Long newGroupId) {
        if (newGroupId == null) {
            return;
        }
        List<PolicyAssignment> groupAssignments =
                assignments.findByTarget(RelationshipType.RESOURCE_GROUP, newGroupId);
        for (PolicyAssignment direct : assignments.findByTarget(RelationshipType.RESOURCE,
                resource.id())) {
            boolean overlaps = groupAssignments.stream().anyMatch(groupAssignment ->
                    groupAssignment.consumerGroupId().equals(direct.consumerGroupId()));
            if (overlaps) {
                throw new AssignmentConflictException("Consumer group " + direct.consumerGroupId()
                        + " has assignments for both resource " + resource.id()
                        + " and resource group " + newGroupId);
            }
        }
    }
}
