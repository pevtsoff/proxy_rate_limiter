package com.example.ratelimiter.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimiter.adapter.out.http.HopByHopHeaders;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HopByHopHeadersTest {

    @Test
    void dropsConnectionScopedHeadersAndKeepsEndToEndOnes() {
        Map<String, List<String>> headers = Map.of(
                "Connection", List.of("keep-alive"),
                "Transfer-Encoding", List.of("chunked"),
                "Host", List.of("proxy:8000"),
                "Content-Length", List.of("12"),
                "Content-Type", List.of("application/json"),
                "X-Request-Id", List.of("abc"));

        assertThat(HopByHopHeaders.forwardableOnly(headers))
                .containsOnlyKeys("Content-Type", "X-Request-Id");
    }
}
