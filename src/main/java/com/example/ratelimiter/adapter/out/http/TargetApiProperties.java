package com.example.ratelimiter.adapter.out.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings of the target API.
 *
 * @param baseUrl     e.g. {@code http://target-api:8001}
 * @param openapiPath path of the OpenAPI JSON document, e.g. {@code /v3/api-docs}
 * @param timeout     connect + response timeout for proxied calls
 * @param syncOnStartup whether resources are imported from OpenAPI when the proxy starts
 */
@ConfigurationProperties(prefix = "target-api")
public record TargetApiProperties(
        String baseUrl, String openapiPath, Duration timeout, boolean syncOnStartup) {

    public String baseUrlWithoutTrailingSlash() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
