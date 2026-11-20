package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.IoType;
import jakarta.persistence.Converter;

/** JPA converter for {@link IoType}. */
@Converter
public class IoTypeConverter extends CodedEnumConverter<IoType> {

    public IoTypeConverter() {
        super(IoType.class);
    }
}
