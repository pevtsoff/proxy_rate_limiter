package com.example.ratelimiter.adapter.out.persistence.converter;

import com.example.ratelimiter.domain.model.HttpMethod;
import jakarta.persistence.Converter;

/** JPA converter for {@link HttpMethod}. */
@Converter
public class HttpMethodConverter extends CodedEnumConverter<HttpMethod> {

    public HttpMethodConverter() {
        super(HttpMethod.class);
    }
}
