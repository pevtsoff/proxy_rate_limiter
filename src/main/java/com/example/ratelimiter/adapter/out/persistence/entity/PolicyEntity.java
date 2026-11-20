package com.example.ratelimiter.adapter.out.persistence.entity;

import com.example.ratelimiter.adapter.out.persistence.converter.RateLimitingStrategyConverter;
import com.example.ratelimiter.adapter.out.persistence.converter.TimePeriodConverter;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** JPA mapping of {@code policies}. */
@Entity
@Table(name = "policies")
public class PolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Convert(converter = RateLimitingStrategyConverter.class)
    @Column(name = "rate_limiting_strategy", nullable = false, length = 32)
    private RateLimitingStrategy strategy;

    @Column(name = "number_of_requests", nullable = false)
    private int numberOfRequests;

    @Convert(converter = TimePeriodConverter.class)
    @Column(name = "time_period", nullable = false, length = 16)
    private TimePeriod timePeriod;

    @Column(name = "is_default", nullable = false, updatable = false)
    private boolean defaultPolicy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PolicyEntity() {
    }

    public static PolicyEntity from(Policy policy) {
        PolicyEntity entity = new PolicyEntity();
        entity.defaultPolicy = policy.defaultPolicy();
        entity.apply(policy);
        return entity;
    }

    /** Copies the editable attributes; identity, version and default flag are kept. */
    public void apply(Policy policy) {
        this.name = policy.name();
        this.strategy = policy.strategy();
        this.numberOfRequests = policy.numberOfRequests();
        this.timePeriod = policy.timePeriod();
    }

    public Policy toDomain() {
        return new Policy(id, name, strategy, numberOfRequests, timePeriod, defaultPolicy);
    }
}
