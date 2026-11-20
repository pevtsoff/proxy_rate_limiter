package com.example.ratelimiter.unit.domain;

import static com.example.ratelimiter.unit.support.TestData.resource;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.service.ResourcePathMatcher;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class ResourcePathMatcherTest {

    private final ResourcePathMatcher matcher = new ResourcePathMatcher();
    private final List<Resource> catalogue = List.of(
            resource(1, HttpMethod.GET, "/orders", 1L),
            resource(2, HttpMethod.GET, "/orders/{id}", 1L),
            resource(3, HttpMethod.GET, "/orders/summary", 1L),
            resource(4, HttpMethod.PATCH, "/orders/{id}", 1L),
            resource(5, HttpMethod.GET, "/users/{userId}/carts/{cartId}", 2L));

    @ParameterizedTest
    @CsvSource({
        "GET, /orders, 1",
        "GET, /orders/, 1",
        "GET, orders, 1",
        "GET, /orders/42, 2",
        "GET, /orders/42?expand=true, 2",
        "GET, /orders/summary, 3",
        "PATCH, /orders/42, 4",
        "GET, /users/7/carts/9, 5"
    })
    void matchesConcretePathsAgainstTemplates(HttpMethod method, String path, long expectedId) {
        assertThat(matcher.match(catalogue, method, path)).map(Resource::id).contains(expectedId);
    }

    @Test
    void literalSegmentsWinOverVariables() {
        assertThat(matcher.match(catalogue, HttpMethod.GET, "/orders/summary"))
                .map(Resource::path).contains("/orders/summary");
    }

    @ParameterizedTest
    @CsvSource({"POST, /orders/1", "GET, /orders/1/items", "GET, /payments", "GET, /"})
    void returnsEmptyWhenNothingMatches(HttpMethod method, String path) {
        assertThat(matcher.match(catalogue, method, path)).isEmpty();
    }
}
