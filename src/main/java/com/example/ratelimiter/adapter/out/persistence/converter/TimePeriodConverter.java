package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.TimePeriod;
import jakarta.persistence.Converter;

/** JPA converter for {@link TimePeriod}. */
@Converter
public class TimePeriodConverter extends CodedEnumConverter<TimePeriod> {

    public TimePeriodConverter() {
        super(TimePeriod.class);
    }
}
