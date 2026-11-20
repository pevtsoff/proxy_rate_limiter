package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.application.port.in.command.CreateConsumerGroupCommand;
import com.example.ratelimiter.application.port.in.command.UpdateConsumerGroupCommand;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;

/** MUC1: CRUD for consumer groups. */
public interface ManageConsumerGroupsUseCase {

    ConsumerGroup create(CreateConsumerGroupCommand command);

    ConsumerGroup update(Long id, UpdateConsumerGroupCommand command);

    /** Deletes the group together with its consumers and policy assignments. */
    void delete(Long id);

    ConsumerGroup get(Long id);

    PageResult<ConsumerGroup> list(PageQuery page);
}
