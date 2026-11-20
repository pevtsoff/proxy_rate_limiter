package com.example.ratelimiter.domain.model;

/** The fixed set of consumer tiers; each tier exists at most once. */
public enum ConsumerGroupName implements CodedEnum {

    PREMIUM("premium"),
    REGULAR("regular"),
    FREE("free"),
    INTERNAL("internal");

    private final String value;

    ConsumerGroupName(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static ConsumerGroupName fromValue(String value) {
        return CodedEnums.fromValue(ConsumerGroupName.class, value);
    }
}
