package com.example.ratelimiter.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimiter.adapter.out.http.OpenApiCatalogAdapter;
import com.example.ratelimiter.adapter.out.http.TargetApiProperties;
import com.example.ratelimiter.domain.model.DiscoveredEndpoint;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** MUC1.4: every (method, path) operation of the OpenAPI document becomes a resource. */
class OpenApiCatalogAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenApiCatalogAdapter adapter = new OpenApiCatalogAdapter(
            HttpClient.newHttpClient(), objectMapper,
            new TargetApiProperties("http://unused", "/v3/api-docs", Duration.ofSeconds(1), false));

    @Test
    void extractsAllOperationsAndIgnoresNonOperationKeys() throws IOException {
        List<DiscoveredEndpoint> endpoints;
        try (InputStream document = getClass().getResourceAsStream("/stub-openapi.json")) {
            endpoints = adapter.parseEndpoints(objectMapper.readTree(document));
        }

        assertThat(endpoints).hasSize(15)
                .contains(new DiscoveredEndpoint(HttpMethod.GET, "/orders"),
                        new DiscoveredEndpoint(HttpMethod.POST, "/orders"),
                        new DiscoveredEndpoint(HttpMethod.PUT, "/users/{id}"),
                        new DiscoveredEndpoint(HttpMethod.PATCH, "/carts/{id}"));
    }
}
