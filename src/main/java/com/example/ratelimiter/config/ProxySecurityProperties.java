package com.example.ratelimiter.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials and token settings.
 *
 * @param jwtSecret     HMAC secret for consumer bearer tokens (at least 32 bytes)
 * @param jwtTtl        lifetime of tokens issued by {@code POST /admin/api/tokens}
 * @param adminUsername admin login for the REST API (HTTP Basic) and the UI (form login)
 * @param adminPassword admin password
 */
@ConfigurationProperties(prefix = "proxy.security")
public record ProxySecurityProperties(
        String jwtSecret, Duration jwtTtl, String adminUsername, String adminPassword) {
}
