package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.application.port.in.command.PolicyCommand;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyFilter;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import java.util.List;

/** MUC4: CRUD for rate-limiting policies. */
public interface ManagePoliciesUseCase {

    Policy create(PolicyCommand command);

    Policy update(Long id, PolicyCommand command);

    /** Deletes a policy; the default policy and policies still assigned cannot be deleted. */
    void delete(Long id);

    Policy get(Long id);

    PageResult<Policy> list(PolicyFilter filter, PageQuery page);

    /** Returns the fallback policy applied when nothing is assigned (MUC3). */
    Policy getDefault();

    /** Returns every strategy the rate limiter can enforce. */
    List<RateLimitingStrategy> supportedStrategies();
}
