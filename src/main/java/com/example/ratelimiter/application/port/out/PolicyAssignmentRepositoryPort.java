package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.AssignmentFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import java.util.List;
import java.util.Optional;

/** Persistence of {@code consumer_groups_policies}. */
public interface PolicyAssignmentRepositoryPort {

    PolicyAssignment save(PolicyAssignment assignment);

    Optional<PolicyAssignment> findById(Long id);

    List<PolicyAssignment> findByConsumerGroupId(Long consumerGroupId);

    List<PolicyAssignment> findByTarget(RelationshipType type, Long targetId);

    PageResult<PolicyAssignment> findAll(AssignmentFilter filter, PageQuery page);

    boolean existsByPolicyId(Long policyId);

    void deleteById(Long id);

    void deleteByConsumerGroupId(Long consumerGroupId);
}
