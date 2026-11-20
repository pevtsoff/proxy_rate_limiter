package com.example.ratelimiter.domain.model;

/** What a policy assignment targets: a single resource or a whole resource group. */
public enum RelationshipType implements CodedEnum {

    RESOURCE("resource"),
    RESOURCE_GROUP("resource_group");

    private final String value;

    RelationshipType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static RelationshipType fromValue(String value) {
        return CodedEnums.fromValue(RelationshipType.class, value);
    }
}
