package com.example.ratelimiter.adapter.out.persistence.repository;

import com.example.ratelimiter.adapter.out.persistence.entity.PolicyAssignmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link PolicyAssignmentEntity}. */
public interface PolicyAssignmentJpaRepository extends JpaRepository<PolicyAssignmentEntity, Long>,
        JpaSpecificationExecutor<PolicyAssignmentEntity> {

    List<PolicyAssignmentEntity> findByConsumerGroupIdOrderById(Long consumerGroupId);

    List<PolicyAssignmentEntity> findByResourceIdOrderById(Long resourceId);

    List<PolicyAssignmentEntity> findByResourceGroupIdOrderById(Long resourceGroupId);

    boolean existsByPolicyId(Long policyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PolicyAssignmentEntity a where a.consumerGroupId = :groupId")
    int deleteByConsumerGroupId(@Param("groupId") Long groupId);
}
