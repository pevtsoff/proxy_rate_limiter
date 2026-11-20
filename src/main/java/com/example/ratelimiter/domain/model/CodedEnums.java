package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;
import java.util.Arrays;
import java.util.List;

/** Lookup helpers shared by all {@link CodedEnum} implementations. */
public final class CodedEnums {

    private CodedEnums() {
    }

    /**
     * Resolves a constant by its external code (case-insensitive).
     *
     * @throws ValidationException when the code is unknown
     */
    public static <E extends Enum<E> & CodedEnum> E fromValue(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.value().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> unsupportedValue(type, value));
    }

    /** Returns the external codes of every constant, in declaration order. */
    public static <E extends Enum<E> & CodedEnum> List<String> values(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(CodedEnum::value).toList();
    }

    private static <E extends Enum<E> & CodedEnum> ValidationException unsupportedValue(
            Class<E> type, String value) {
        return new ValidationException(
                "Unsupported " + type.getSimpleName() + " '" + value + "'. Supported values: "
                        + values(type));
    }
}
