package com.example.ratelimiter.adapter.out.persistence.repository;

import com.example.ratelimiter.adapter.out.persistence.entity.ConsumerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link ConsumerEntity}. */
public interface ConsumerJpaRepository
        extends JpaRepository<ConsumerEntity, Long>, JpaSpecificationExecutor<ConsumerEntity> {

    Optional<ConsumerEntity> findByConsumerId(String consumerId);

    long countByConsumerGroupId(Long consumerGroupId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ConsumerEntity c where c.consumerGroupId = :groupId")
    int deleteByConsumerGroupId(@Param("groupId") Long groupId);
}
