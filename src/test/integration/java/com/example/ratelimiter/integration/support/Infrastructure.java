package com.example.ratelimiter.integration.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL and Redis (Testcontainers) plus an in-process stub of the target API. They are
 * started once per JVM and shared by every integration test (Ryuk removes the containers).
 */
public final class Infrastructure {

    public static final String DATABASE = "proxy_test";
    public static final String DB_USER = "proxy";
    public static final String DB_PASSWORD = "proxy";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin-test-password";

    public static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withEnv("POSTGRES_DB", DATABASE)
                    .withEnv("POSTGRES_USER", DB_USER)
                    .withEnv("POSTGRES_PASSWORD", DB_PASSWORD)
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(
                            ".*database system is ready to accept connections.*\\s", 2));

    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\s", 1));

    public static final StubTargetApi TARGET_API = new StubTargetApi();

    static {
        POSTGRES.start();
        REDIS.start();
        TARGET_API.start();
    }

    private Infrastructure() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost()
                + ":" + POSTGRES.getMappedPort(5432) + "/" + DATABASE);
        registry.add("spring.datasource.username", () -> DB_USER);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("target-api.base-url", TARGET_API::baseUrl);
        registry.add("target-api.sync-on-startup", () -> "false");
        registry.add("proxy.security.jwt-secret", () -> "integration-test-secret-0123456789abcdef");
        registry.add("proxy.security.admin-username", () -> ADMIN_USERNAME);
        registry.add("proxy.security.admin-password", () -> ADMIN_PASSWORD);
    }
}
