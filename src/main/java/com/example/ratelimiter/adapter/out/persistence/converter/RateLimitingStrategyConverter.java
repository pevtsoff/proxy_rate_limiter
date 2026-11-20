package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import jakarta.persistence.Converter;

/** JPA converter for {@link RateLimitingStrategy}. */
@Converter
public class RateLimitingStrategyConverter extends CodedEnumConverter<RateLimitingStrategy> {

    public RateLimitingStrategyConverter() {
        super(RateLimitingStrategy.class);
    }
}
