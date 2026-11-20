package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.ConsumerGroupName;
import jakarta.persistence.Converter;

/** JPA converter for {@link ConsumerGroupName}. */
@Converter
public class ConsumerGroupNameConverter extends CodedEnumConverter<ConsumerGroupName> {

    public ConsumerGroupNameConverter() {
        super(ConsumerGroupName.class);
    }
}
