package com.example.ratelimiter.unit.support;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.TimePeriod;

/** Builders for domain objects used across unit tests. */
public final class TestData {

    private TestData() {
    }

    public static Policy policy(long id, int requests) {
        return new Policy(id, "policy-" + id, RateLimitingStrategy.MOVING_WINDOW, requests,
                TimePeriod.MINUTE, false);
    }

    public static Policy defaultPolicy() {
        return new Policy(1L, "default", RateLimitingStrategy.MOVING_WINDOW, 60,
                TimePeriod.MINUTE, true);
    }

    public static Resource resource(long id, HttpMethod method, String path, Long groupId) {
        return new Resource(id, method, path, null, groupId);
    }

    public static PolicyAssignment onResource(long id, long consumerGroupId, long resourceId) {
        return new PolicyAssignment(id, consumerGroupId, RelationshipType.RESOURCE, 10L,
                resourceId);
    }

    public static PolicyAssignment onGroup(long id, long consumerGroupId, long resourceGroupId) {
        return new PolicyAssignment(
                id, consumerGroupId, RelationshipType.RESOURCE_GROUP, 10L, resourceGroupId);
    }
}
