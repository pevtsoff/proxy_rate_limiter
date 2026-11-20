package com.example.ratelimiter.adapter.out.persistence.repository;

import com.example.ratelimiter.adapter.out.persistence.entity.ResourceGroupEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ResourceGroupEntity}. */
public interface ResourceGroupJpaRepository extends JpaRepository<ResourceGroupEntity, Long> {

    Optional<ResourceGroupEntity> findByName(String name);
}
