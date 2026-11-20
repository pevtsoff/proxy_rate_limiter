package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.application.port.in.command.ConsumerCommand;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerFilter;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;

/** MUC1: CRUD for consumers; every consumer belongs to exactly one consumer group. */
public interface ManageConsumersUseCase {

    Consumer create(ConsumerCommand command);

    Consumer update(Long id, ConsumerCommand command);

    /** Deletes a consumer unless it is the last consumer of its group. */
    void delete(Long id);

    Consumer get(Long id);

    PageResult<Consumer> list(ConsumerFilter filter, PageQuery page);
}
