package com.example.ratelimiter.integration.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.adapter.out.cache.RuleCacheNames;
import com.example.ratelimiter.integration.support.IntegrationTest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/** MUC5: LRU cache of (consumer_id, rest_path) -> policy, invalidated on configuration CRUD. */
@Transactional
@Sql(IntegrationTest.SEED)
class RuleCacheIT extends IntegrationTest {

    @Test
    void test_muc5_cacheIsBoundedAndExpiresAsConfigured() {
        Cache<Object, Object> cache = nativeCache();
        assertThat(cache.policy().eviction()).hasValueSatisfying(
                eviction -> assertThat(eviction.getMaximum()).isEqualTo(1000));
        assertThat(cache.policy().expireAfterWrite()).hasValueSatisfying(
                expiry -> assertThat(expiry.getExpiresAfter(TimeUnit.SECONDS)).isEqualTo(3600));
        assertThat(cache.policy().isRecordingStats()).isTrue();
    }

    @Test
    void test_muc5_repeatedRequestsAreResolvedFromCache() throws Exception {
        String token = tokenFor("free-user-1");
        CacheStats before = nativeCache().stats();

        consumerCall(get("/orders/1"), token).andExpect(status().isOk());
        consumerCall(get("/orders/1"), token).andExpect(status().isOk());
        consumerCall(get("/orders/1"), token).andExpect(status().isOk());

        CacheStats delta = nativeCache().stats().minus(before);
        assertThat(delta.missCount()).isEqualTo(1);
        assertThat(delta.hitCount()).isEqualTo(2);
        assertThat(nativeCache().estimatedSize()).isEqualTo(1);
    }

    @Test
    void test_muc5_policyUpdateEvictsCacheAndTakesEffectImmediately() throws Exception {
        String token = tokenFor("free-user-1");
        consumerCall(get("/orders/1"), token)
                .andExpect(header().string("X-RateLimit-Limit", "10"));
        assertThat(nativeCache().estimatedSize()).isEqualTo(1);

        adminPut("/admin/api/policies/" + policyId("free-10-per-minute"), Map.of(
                "name", "free-10-per-minute", "rate_limiting_strategy", "fixed-window",
                "number_of_requests", 2, "time_period", "minute"));

        assertThat(nativeCache().estimatedSize()).isZero();
        consumerCall(get("/orders/1"), token).andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "2"));
        consumerCall(get("/orders/1"), token).andExpect(status().isOk());
        consumerCall(get("/orders/1"), token).andExpect(status().isTooManyRequests());
    }

    @Test
    void test_muc5_assignmentChangeEvictsCache() throws Exception {
        String token = tokenFor("free-user-1");
        consumerCall(get("/users/1"), token)
                .andExpect(header().string("X-RateLimit-Policy", "default;w=60"));

        createAndGetId("/admin/api/consumer-groups-policies", Map.of(
                "consumer_group_id", consumerGroupId("free"), "relationship_type", "resource",
                "policy_id", policyId("free-10-per-minute"),
                "resource_id", resourceId("GET", "/users/{id}")));

        consumerCall(get("/users/1"), token)
                .andExpect(header().string("X-RateLimit-Policy", "free-10-per-minute;w=60"));
    }

    @SuppressWarnings("unchecked")
    private Cache<Object, Object> nativeCache() {
        CaffeineCache cache = (CaffeineCache) Objects.requireNonNull(
                cacheManager.getCache(RuleCacheNames.APPLICABLE_RULES));
        return cache.getNativeCache();
    }
}
