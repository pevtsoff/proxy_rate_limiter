package com.example.ratelimiter.domain.model;

/** HTTP request methods a target resource can be exposed with. */
public enum HttpMethod implements CodedEnum {

    GET(IoType.READ),
    HEAD(IoType.READ),
    OPTIONS(IoType.READ),
    TRACE(IoType.READ),
    POST(IoType.WRITE),
    PUT(IoType.WRITE),
    PATCH(IoType.WRITE),
    DELETE(IoType.WRITE);

    private final IoType defaultIoType;

    HttpMethod(IoType defaultIoType) {
        this.defaultIoType = defaultIoType;
    }

    @Override
    public String value() {
        return name();
    }

    /** Safe methods are {@code read}, every state-changing method is {@code write}. */
    public IoType defaultIoType() {
        return defaultIoType;
    }

    public static HttpMethod fromValue(String value) {
        return CodedEnums.fromValue(HttpMethod.class, value);
    }
}
