package com.example.ratelimiter.domain.model;

import java.time.Duration;

/** Period over which {@code number_of_requests} of a policy is counted. */
public enum TimePeriod implements CodedEnum {

    SECOND("second", Duration.ofSeconds(1)),
    MINUTE("minute", Duration.ofMinutes(1)),
    HOUR("hour", Duration.ofHours(1)),
    DAY("day", Duration.ofDays(1)),
    WEEK("week", Duration.ofDays(7));

    private final String value;
    private final Duration duration;

    TimePeriod(String value, Duration duration) {
        this.value = value;
        this.duration = duration;
    }

    @Override
    public String value() {
        return value;
    }

    public Duration duration() {
        return duration;
    }

    public static TimePeriod fromValue(String value) {
        return CodedEnums.fromValue(TimePeriod.class, value);
    }
}
