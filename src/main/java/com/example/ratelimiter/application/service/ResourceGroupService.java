package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ManageResourceGroupsUseCase;
import com.example.ratelimiter.application.port.in.command.ResourceGroupCommand;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.ResourceGroup;

/** CRUD for resource groups. */
public final class ResourceGroupService implements ManageResourceGroupsUseCase {

    private static final String ENTITY = "Resource group";

    private final ResourceGroupRepositoryPort resourceGroups;
    private final PolicyAssignmentRepositoryPort assignments;
    private final UnitOfWorkPort unitOfWork;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    public ResourceGroupService(
            ResourceGroupRepositoryPort resourceGroups,
            PolicyAssignmentRepositoryPort assignments,
            UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.resourceGroups = resourceGroups;
        this.assignments = assignments;
        this.unitOfWork = unitOfWork;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public ResourceGroup create(ResourceGroupCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            ensureNameAvailable(command.name(), null);
            return resourceGroups.save(ResourceGroup.newGroup(command.name()));
        });
    }

    @Override
    public ResourceGroup update(Long id, ResourceGroupCommand command) {
        return ruleAffectingTransaction.execute(() -> {
            load(id);
            ensureNameAvailable(command.name(), id);
            return resourceGroups.save(new ResourceGroup(id, command.name()));
        });
    }

    @Override
    public void delete(Long id) {
        ruleAffectingTransaction.run(() -> {
            load(id);
            if (!assignments.findByTarget(RelationshipType.RESOURCE_GROUP, id).isEmpty()) {
                throw new ConflictException(
                        "Resource group " + id
                                + " is still assigned; delete its assignments first");
            }
            resourceGroups.deleteById(id);
        });
    }

    @Override
    public ResourceGroup get(Long id) {
        return unitOfWork.inReadOnlyTransaction(() -> load(id));
    }

    @Override
    public PageResult<ResourceGroup> list(PageQuery page) {
        return unitOfWork.inReadOnlyTransaction(() -> resourceGroups.findAll(page));
    }

    private ResourceGroup load(Long id) {
        return resourceGroups.findById(id).orElseThrow(() -> new NotFoundException(ENTITY, id));
    }

    private void ensureNameAvailable(String name, Long ownId) {
        resourceGroups.findByName(name)
                .filter(other -> !other.id().equals(ownId))
                .ifPresent(other -> {
                    throw new ConflictException("Resource group '" + name + "' already exists");
                });
    }
}
