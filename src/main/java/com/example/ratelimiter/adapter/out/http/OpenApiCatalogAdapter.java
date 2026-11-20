package com.example.ratelimiter.adapter.out.http;

import com.example.ratelimiter.application.port.out.TargetApiCatalogPort;
import com.example.ratelimiter.domain.exception.UpstreamUnavailableException;
import com.example.ratelimiter.domain.model.DiscoveredEndpoint;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads the target's OpenAPI 3 document (architectural assumption N2) and lists every
 * {@code (method, path)} operation it declares.
 */
@Component
public class OpenApiCatalogAdapter implements TargetApiCatalogPort {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TargetApiProperties properties;

    public OpenApiCatalogAdapter(
            HttpClient httpClient, ObjectMapper objectMapper, TargetApiProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<DiscoveredEndpoint> fetchEndpoints() {
        return parseEndpoints(fetchDocument());
    }

    /** Extracts every operation from an OpenAPI document; visible for unit tests. */
    public List<DiscoveredEndpoint> parseEndpoints(JsonNode document) {
        List<DiscoveredEndpoint> endpoints = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : document.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                toHttpMethod(operation.getKey()).ifPresent(method ->
                        endpoints.add(new DiscoveredEndpoint(method, path.getKey())));
            }
        }
        return endpoints;
    }

    private JsonNode fetchDocument() {
        URI uri = URI.create(properties.baseUrlWithoutTrailingSlash() + properties.openapiPath());
        HttpRequest request =
                HttpRequest.newBuilder(uri).timeout(properties.timeout()).GET().build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccessful(uri, response.statusCode());
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new UpstreamUnavailableException(
                    "Could not fetch OpenAPI document from " + uri, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamUnavailableException("Interrupted while fetching " + uri, exception);
        }
    }

    private static void ensureSuccessful(URI uri, int status) {
        if (status < 200 || status >= 300) {
            throw new UpstreamUnavailableException(
                    "OpenAPI document " + uri + " answered HTTP " + status, null);
        }
    }

    /** OpenAPI path items also contain non-operation keys such as {@code parameters}. */
    private static Optional<HttpMethod> toHttpMethod(String key) {
        return Arrays.stream(HttpMethod.values())
                .filter(method -> method.value().equalsIgnoreCase(key))
                .findFirst();
    }
}
