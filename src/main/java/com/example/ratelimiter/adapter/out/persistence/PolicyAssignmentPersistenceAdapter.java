package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.adapter.out.persistence.entity.PolicyAssignmentEntity;
import com.example.ratelimiter.adapter.out.persistence.repository.PolicyAssignmentJpaRepository;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.AssignmentFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link PolicyAssignmentRepositoryPort} backed by PostgreSQL. */
@Component
public class PolicyAssignmentPersistenceAdapter implements PolicyAssignmentRepositoryPort {

    private final PolicyAssignmentJpaRepository repository;

    public PolicyAssignmentPersistenceAdapter(PolicyAssignmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PolicyAssignment save(PolicyAssignment assignment) {
        PolicyAssignmentEntity entity = assignment.id() == null
                ? PolicyAssignmentEntity.from(assignment)
                : repository.findById(assignment.id()).orElseThrow(
                        () -> new NotFoundException("Policy assignment", assignment.id()));
        entity.apply(assignment);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<PolicyAssignment> findById(Long id) {
        return repository.findById(id).map(PolicyAssignmentEntity::toDomain);
    }

    @Override
    public List<PolicyAssignment> findByConsumerGroupId(Long consumerGroupId) {
        return toDomain(repository.findByConsumerGroupIdOrderById(consumerGroupId));
    }

    @Override
    public List<PolicyAssignment> findByTarget(RelationshipType type, Long targetId) {
        return type == RelationshipType.RESOURCE
                ? toDomain(repository.findByResourceIdOrderById(targetId))
                : toDomain(repository.findByResourceGroupIdOrderById(targetId));
    }

    @Override
    public PageResult<PolicyAssignment> findAll(AssignmentFilter filter, PageQuery page) {
        var specification = new Specifications<PolicyAssignmentEntity>()
                .equal("consumerGroupId", filter.consumerGroupId())
                .equal("relationshipType", filter.relationshipType())
                .equal("policyId", filter.policyId())
                .build();
        return Pages.toPageResult(repository.findAll(specification, Pages.toPageRequest(page)),
                PolicyAssignmentEntity::toDomain);
    }

    @Override
    public boolean existsByPolicyId(Long policyId) {
        return repository.existsByPolicyId(policyId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByConsumerGroupId(Long consumerGroupId) {
        repository.deleteByConsumerGroupId(consumerGroupId);
    }

    private static List<PolicyAssignment> toDomain(List<PolicyAssignmentEntity> entities) {
        return entities.stream().map(PolicyAssignmentEntity::toDomain).toList();
    }
}
