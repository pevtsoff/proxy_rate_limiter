package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.CodedEnum;
import com.example.ratelimiter.domain.model.CodedEnums;
import jakarta.persistence.AttributeConverter;

/** Stores a {@link CodedEnum} by its external code, e.g. {@code moving-window}. */
public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected CodedEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CodedEnums.fromValue(type, dbData);
    }
}
