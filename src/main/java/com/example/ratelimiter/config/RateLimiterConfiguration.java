package com.example.ratelimiter.config;

import com.example.ratelimiter.adapter.out.ratelimit.Bandwidths;
import com.example.ratelimiter.adapter.out.ratelimit.Bucket4jAlgorithm;
import com.example.ratelimiter.adapter.out.ratelimit.RateLimitAlgorithm;
import com.example.ratelimiter.adapter.out.ratelimit.RedisRateLimiterAdapter;
import com.example.ratelimiter.adapter.out.ratelimit.SlidingWindowLogAlgorithm;
import com.example.ratelimiter.application.port.out.RateLimiterPort;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** Wires the Redis-backed rate-limiting algorithms (Bucket4j over Lettuce + Lua script). */
@Configuration(proxyBeanMethods = false)
public class RateLimiterConfiguration {

    /** Idle buckets are removed from Redis this long after they would be full again. */
    private static final Duration BUCKET_EXPIRY_GRACE = Duration.ofSeconds(10);

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port) {
        return RedisClient.create(RedisURI.builder().withHost(host).withPort(port).build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient client) {
        return client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> bucket4jProxyManager(
            StatefulRedisConnection<String, byte[]> connection) {
        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(BUCKET_EXPIRY_GRACE))
                .build();
    }

    @Bean
    public RateLimitAlgorithm movingWindowAlgorithm(StringRedisTemplate redis) {
        RedisScript<String> script = RedisScript.of(
                new ClassPathResource("redis/sliding_window_log.lua"), String.class);
        return new SlidingWindowLogAlgorithm(redis, script);
    }

    @Bean
    public RateLimitAlgorithm fixedWindowAlgorithm(ProxyManager<String> proxyManager) {
        return new Bucket4jAlgorithm(
                RateLimitingStrategy.FIXED_WINDOW, proxyManager, Bandwidths::fixedWindow);
    }

    @Bean
    public RateLimitAlgorithm tokenBucketAlgorithm(ProxyManager<String> proxyManager) {
        return new Bucket4jAlgorithm(
                RateLimitingStrategy.TOKEN_BUCKET, proxyManager, Bandwidths::tokenBucket);
    }

    @Bean
    public RateLimiterPort rateLimiterPort(List<RateLimitAlgorithm> algorithms) {
        return new RedisRateLimiterAdapter(algorithms);
    }
}
