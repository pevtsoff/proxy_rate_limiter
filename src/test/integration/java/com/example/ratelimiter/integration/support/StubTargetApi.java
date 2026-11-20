package com.example.ratelimiter.integration.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal in-process target API: serves the OpenAPI document of the real target and echoes
 * every other request (method, path, query, consumer header, body) as JSON. It also counts the
 * requests it receives, so tests can prove that rejected requests never reach the target.
 */
public final class StubTargetApi {

    private final AtomicInteger received = new AtomicInteger();
    private HttpServer server;

    void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        server.createContext("/v3/api-docs", this::openApi);
        server.createContext("/", this::echo);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public int receivedRequests() {
        return received.get();
    }

    public void resetCounter() {
        received.set(0);
    }

    private void openApi(HttpExchange exchange) throws IOException {
        try (InputStream document = getClass().getResourceAsStream("/stub-openapi.json")) {
            respond(exchange, 200, document.readAllBytes());
        }
    }

    private void echo(HttpExchange exchange) throws IOException {
        received.incrementAndGet();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String json = String.format(
                "{\"method\":\"%s\",\"path\":\"%s\",\"query\":\"%s\",\"consumer\":\"%s\","
                        + "\"authorization\":\"%s\",\"body_length\":%d}",
                exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders().getFirst("X-Consumer-Id"),
                exchange.getRequestHeaders().getFirst("Authorization"), body.length());
        exchange.getResponseHeaders().add("X-Target", "stub");
        respond(exchange, 200, json.getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
