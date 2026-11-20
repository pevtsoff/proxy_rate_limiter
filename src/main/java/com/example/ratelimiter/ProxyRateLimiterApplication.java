package com.example.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Proxy Rate Limiter: a reverse proxy that enforces configurable, Redis-backed rate limits in
 * front of an arbitrary REST API (architectural assumption N1).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ProxyRateLimiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyRateLimiterApplication.class, args);
    }
}
