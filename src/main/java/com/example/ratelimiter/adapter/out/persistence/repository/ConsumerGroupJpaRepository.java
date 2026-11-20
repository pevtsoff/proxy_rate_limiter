package com.example.ratelimiter.adapter.out.persistence.repository;

import com.example.ratelimiter.adapter.out.persistence.entity.ConsumerGroupEntity;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link ConsumerGroupEntity}. */
public interface ConsumerGroupJpaRepository extends JpaRepository<ConsumerGroupEntity, Long> {

    Optional<ConsumerGroupEntity> findByName(ConsumerGroupName name);

    /** {@code SELECT ... FOR UPDATE}: serialises changes that concern one consumer group. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from ConsumerGroupEntity g where g.id = :id")
    Optional<ConsumerGroupEntity> findByIdForUpdate(@Param("id") Long id);
}
