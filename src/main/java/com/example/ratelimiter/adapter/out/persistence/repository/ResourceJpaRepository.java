package com.example.ratelimiter.adapter.out.persistence.repository;

import com.example.ratelimiter.adapter.out.persistence.entity.ResourceEntity;
import com.example.ratelimiter.domain.model.HttpMethod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link ResourceEntity}. */
public interface ResourceJpaRepository
        extends JpaRepository<ResourceEntity, Long>, JpaSpecificationExecutor<ResourceEntity> {

    Optional<ResourceEntity> findByMethodAndPath(HttpMethod method, String path);
}
