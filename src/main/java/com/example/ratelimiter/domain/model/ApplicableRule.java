package com.example.ratelimiter.domain.model;

/**
 * The policy that applies to one {@code (consumer, method, path)} combination, plus the scope
 * its counter is shared across.
 *
 * @param policy  policy to enforce
 * @param scope   counter scope, e.g. {@code resource:12} or {@code resource-group:3}; requests of
 *                the same consumer in the same scope consume from the same budget
 * @param matched {@code true} when the request matched a known target resource
 */
public record ApplicableRule(Policy policy, String scope, boolean matched) {

    private static final String KEY_PREFIX = "rl";
    private static final String KEY_SEPARATOR = ":";

    public static ApplicableRule forResource(Policy policy, Resource resource) {
        return new ApplicableRule(policy, "resource:" + resource.id(), true);
    }

    public static ApplicableRule forResourceGroup(Policy policy, Long resourceGroupId) {
        return new ApplicableRule(policy, "resource-group:" + resourceGroupId, true);
    }

    public static ApplicableRule defaultForResource(Policy defaultPolicy, Resource resource) {
        return new ApplicableRule(defaultPolicy, "default:resource:" + resource.id(), true);
    }

    /** Unknown paths share one default budget so that varying ids cannot bypass the limit. */
    public static ApplicableRule defaultForUnmatched(Policy defaultPolicy) {
        return new ApplicableRule(defaultPolicy, "default:unmatched", false);
    }

    /** Builds the distributed counter key for the given consumer. */
    public String bucketKey(String consumerId) {
        return String.join(KEY_SEPARATOR, KEY_PREFIX, consumerId, scope, policy.fingerprint());
    }
}
