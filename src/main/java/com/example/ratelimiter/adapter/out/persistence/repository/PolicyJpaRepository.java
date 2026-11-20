package com.example.ratelimiter.adapter.out.persistence.repository;

import com.example.ratelimiter.adapter.out.persistence.entity.PolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link PolicyEntity}. */
public interface PolicyJpaRepository
        extends JpaRepository<PolicyEntity, Long>, JpaSpecificationExecutor<PolicyEntity> {

    Optional<PolicyEntity> findByName(String name);

    Optional<PolicyEntity> findByDefaultPolicyTrue();
}
