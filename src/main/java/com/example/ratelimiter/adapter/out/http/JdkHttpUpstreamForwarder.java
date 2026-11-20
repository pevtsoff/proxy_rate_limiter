package com.example.ratelimiter.adapter.out.http;

import com.example.ratelimiter.application.port.out.UpstreamForwarderPort;
import com.example.ratelimiter.domain.exception.UpstreamTimeoutException;
import com.example.ratelimiter.domain.exception.UpstreamUnavailableException;
import com.example.ratelimiter.domain.model.ProxiedRequest;
import com.example.ratelimiter.domain.model.UpstreamResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Forwards requests with the JDK {@link HttpClient}. Calls are blocking, which is cheap because
 * the proxy runs request handling on virtual threads.
 */
@Component
public class JdkHttpUpstreamForwarder implements UpstreamForwarderPort {

    static final String CONSUMER_ID_HEADER = "X-Consumer-Id";

    private final HttpClient httpClient;
    private final TargetApiProperties properties;

    public JdkHttpUpstreamForwarder(HttpClient httpClient, TargetApiProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    public UpstreamResponse forward(ProxiedRequest request) {
        HttpRequest upstreamRequest = toUpstreamRequest(request);
        try {
            HttpResponse<byte[]> response =
                    httpClient.send(upstreamRequest, HttpResponse.BodyHandlers.ofByteArray());
            return new UpstreamResponse(response.statusCode(),
                    HopByHopHeaders.forwardableOnly(response.headers().map()), response.body());
        } catch (HttpTimeoutException exception) {
            throw new UpstreamTimeoutException(
                    "Target API did not answer within " + properties.timeout(), exception);
        } catch (IOException exception) {
            throw new UpstreamUnavailableException("Target API is unreachable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamUnavailableException("Interrupted while calling target API",
                    exception);
        }
    }

    private HttpRequest toUpstreamRequest(ProxiedRequest request) {
        URI uri = URI.create(properties.baseUrlWithoutTrailingSlash() + request.pathWithQuery());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(properties.timeout())
                .method(request.method().value(), bodyOf(request));
        copyHeaders(request.headers(), builder);
        builder.setHeader(CONSUMER_ID_HEADER, request.consumerId());
        return builder.build();
    }

    private static HttpRequest.BodyPublisher bodyOf(ProxiedRequest request) {
        return request.body().length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(request.body());
    }

    private static void copyHeaders(Map<String, List<String>> headers,
            HttpRequest.Builder builder) {
        HopByHopHeaders.forwardableOnly(headers).forEach((name, values) ->
                values.forEach(value -> builder.header(name, value)));
    }
}
