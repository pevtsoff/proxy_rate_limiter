package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import java.util.Optional;

/** Persistence of consumer groups. */
public interface ConsumerGroupRepositoryPort {

    ConsumerGroup save(ConsumerGroup group);

    Optional<ConsumerGroup> findById(Long id);

    /**
     * Loads the group and locks its row until the surrounding transaction ends. Used to
     * serialise concurrent changes of the group's consumers and assignments.
     */
    Optional<ConsumerGroup> findByIdForUpdate(Long id);

    Optional<ConsumerGroup> findByName(ConsumerGroupName name);

    PageResult<ConsumerGroup> findAll(PageQuery page);

    void deleteById(Long id);
}
