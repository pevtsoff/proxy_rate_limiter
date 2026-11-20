package com.example.ratelimiter.domain.model;

/** Whether a target resource reads or mutates state. */
public enum IoType implements CodedEnum {

    READ("read"),
    WRITE("write");

    private final String value;

    IoType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static IoType fromValue(String value) {
        return CodedEnums.fromValue(IoType.class, value);
    }
}
