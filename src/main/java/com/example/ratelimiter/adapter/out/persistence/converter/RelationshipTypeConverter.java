package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.RelationshipType;
import jakarta.persistence.Converter;

/** JPA converter for {@link RelationshipType}. */
@Converter
public class RelationshipTypeConverter extends CodedEnumConverter<RelationshipType> {

    public RelationshipTypeConverter() {
        super(RelationshipType.class);
    }
}
