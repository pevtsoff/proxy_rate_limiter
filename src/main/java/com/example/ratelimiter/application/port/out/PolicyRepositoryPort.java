package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyFilter;
import java.util.Optional;

/** Persistence of policies. */
public interface PolicyRepositoryPort {

    Policy save(Policy policy);

    Optional<Policy> findById(Long id);

    Optional<Policy> findByName(String name);

    Optional<Policy> findDefault();

    PageResult<Policy> findAll(PolicyFilter filter, PageQuery page);

    void deleteById(Long id);
}
