package com.example.ratelimiter.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ratelimiter.domain.exception.ValidationException;
import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourcePaths;
import com.example.ratelimiter.domain.model.TimePeriod;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void policyDefaultsToMovingWindowAndValidatesLimits() {
        Policy policy = Policy.newPolicy("p", null, 5, TimePeriod.SECOND);
        assertThat(policy.strategy()).isEqualTo(RateLimitingStrategy.MOVING_WINDOW);
        assertThatThrownBy(() -> Policy.newPolicy("p", null, 0, TimePeriod.SECOND))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Policy.newPolicy(" ", null, 1, TimePeriod.SECOND))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Policy.newPolicy("p", null, 1, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void editingAPolicyChangesItsCounterKey() {
        Policy original = new Policy(3L, "p", RateLimitingStrategy.TOKEN_BUCKET, 10,
                TimePeriod.MINUTE, false);
        Policy edited = original.withDefinition("p", RateLimitingStrategy.TOKEN_BUCKET, 20,
                TimePeriod.MINUTE);
        Resource resource = new Resource(9L, HttpMethod.GET, "/orders", null, null);
        assertThat(ApplicableRule.forResource(original, resource).bucketKey("alice"))
                .isEqualTo("rl:alice:resource:9:3-token-bucket-10-minute")
                .isNotEqualTo(ApplicableRule.forResource(edited, resource).bucketKey("alice"));
    }

    @Test
    void resourceDerivesIoTypeFromMethodAndNormalisesPath() {
        assertThat(Resource.discovered(HttpMethod.GET, "orders/", 1L))
                .extracting(Resource::ioType, Resource::path)
                .containsExactly(IoType.READ, "/orders");
        assertThat(Resource.discovered(HttpMethod.POST, "/orders", 1L).ioType())
                .isEqualTo(IoType.WRITE);
        assertThat(Resource.discovered(HttpMethod.PUT, "/orders", 1L).ioType())
                .isEqualTo(IoType.WRITE);
        assertThat(Resource.discovered(HttpMethod.PATCH, "/orders", 1L).ioType())
                .isEqualTo(IoType.WRITE);
    }

    @Test
    void consumerRequiresAConsumerGroup() {
        assertThatThrownBy(() -> Consumer.newConsumer("alice", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("consumer group");
    }

    @Test
    void codedEnumsUseExternalValues() {
        assertThat(RateLimitingStrategy.fromValue("fixed-window"))
                .isEqualTo(RateLimitingStrategy.FIXED_WINDOW);
        assertThat(RelationshipType.fromValue("resource_group"))
                .isEqualTo(RelationshipType.RESOURCE_GROUP);
        assertThat(TimePeriod.WEEK.duration()).isEqualTo(Duration.ofDays(7));
        assertThatThrownBy(() -> RateLimitingStrategy.fromValue("leaky"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("moving-window");
    }

    @Test
    void retryAfterIsRoundedUpToWholeSeconds() {
        assertThat(RateLimitDecision.rejected(5, Duration.ofMillis(1200)).retryAfterSeconds())
                .isEqualTo(2);
        assertThat(RateLimitDecision.rejected(5, Duration.ZERO).retryAfterSeconds()).isEqualTo(1);
        assertThat(RateLimitDecision.allowed(5, 4).retryAfterSeconds()).isZero();
    }

    @Test
    void resourcePathHelpers() {
        assertThat(ResourcePaths.normalize("//orders//1/")).isEqualTo("/orders/1");
        assertThat(ResourcePaths.firstSegment("/orders/{id}")).isEqualTo("orders");
        assertThat(ResourcePaths.withoutLeadingSlash("/orders/1")).isEqualTo("orders/1");
    }
}
