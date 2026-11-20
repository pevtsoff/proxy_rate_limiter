package com.example.ratelimiter.integration.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.ratelimiter.adapter.out.ratelimit.Bandwidths;
import com.example.ratelimiter.adapter.out.ratelimit.Bucket4jAlgorithm;
import com.example.ratelimiter.adapter.out.ratelimit.RedisRateLimiterAdapter;
import com.example.ratelimiter.adapter.out.ratelimit.SlidingWindowLogAlgorithm;
import com.example.ratelimiter.application.port.out.RateLimiterPort;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.RateLimitDecision;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import com.example.ratelimiter.integration.support.Infrastructure;
import com.example.ratelimiter.integration.support.IntegrationTest;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.jdbc.Sql;

/**
 * CUC1.4 under concurrency: the limit must hold exactly for parallel requests and across
 * application instances. These tests commit their data (threads cannot see an uncommitted test
 * transaction), so the database is cleaned explicitly afterwards.
 */
@Sql(IntegrationTest.SEED)
@Sql(scripts = IntegrationTest.CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DistributedRateLimitIT extends IntegrationTest {

    private static final int PARALLEL_REQUESTS = 60;
    private static RedisClient secondInstanceClient;
    private static LettuceConnectionFactory secondInstanceConnectionFactory;
    private static RateLimiterPort secondInstance;

    @Autowired
    private RateLimiterPort firstInstance;

    @BeforeAll
    static void startSecondInstance() {
        String host = Infrastructure.REDIS.getHost();
        int port = Infrastructure.REDIS.getMappedPort(6379);
        secondInstanceClient = RedisClient.create(RedisURI.create(host, port));
        StatefulRedisConnection<String, byte[]> connection = secondInstanceClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        ProxyManager<String> proxyManager = Bucket4jLettuce.casBasedBuilder(connection).build();
        secondInstanceConnectionFactory = new LettuceConnectionFactory(host, port);
        secondInstanceConnectionFactory.afterPropertiesSet();
        secondInstanceConnectionFactory.start();
        StringRedisTemplate template = new StringRedisTemplate(secondInstanceConnectionFactory);
        secondInstance = new RedisRateLimiterAdapter(List.of(
                new SlidingWindowLogAlgorithm(template, RedisScript.of(
                        new ClassPathResource("redis/sliding_window_log.lua"), String.class)),
                new Bucket4jAlgorithm(RateLimitingStrategy.FIXED_WINDOW, proxyManager,
                        Bandwidths::fixedWindow),
                new Bucket4jAlgorithm(RateLimitingStrategy.TOKEN_BUCKET, proxyManager,
                        Bandwidths::tokenBucket)));
    }

    @AfterAll
    static void stopSecondInstance() {
        secondInstanceConnectionFactory.destroy();
        secondInstanceClient.shutdown();
    }

    @Test
    void test_cuc1_parallelRequestsNeverExceedTheLimit() throws Exception {
        String token = tokenFor("free-user-1");
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int request = 0; request < PARALLEL_REQUESTS; request++) {
            calls.add(() -> consumerCall(get("/orders/1"), token)
                    .andReturn().getResponse().getStatus());
        }

        List<Integer> statuses = runConcurrently(calls);

        assertThat(statuses).filteredOn(status -> status == 200).hasSize(10);
        assertThat(statuses).filteredOn(status -> status == 429).hasSize(PARALLEL_REQUESTS - 10);
        assertThat(Infrastructure.TARGET_API.receivedRequests()).isEqualTo(10);
    }

    @ParameterizedTest
    @EnumSource(RateLimitingStrategy.class)
    void test_cuc1_limitHoldsAcrossTwoProxyInstances(RateLimitingStrategy strategy)
            throws Exception {
        // An hourly period keeps token-bucket refill negligible while the test runs.
        Policy policy = new Policy(900L, "shared", strategy, 25, TimePeriod.HOUR, false);
        String key = "rl:shared-consumer:resource:1:" + policy.fingerprint();
        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int attempt = 0; attempt < 100; attempt++) {
            RateLimiterPort instance = attempt % 2 == 0 ? firstInstance : secondInstance;
            calls.add(() -> instance.tryConsume(key, policy).allowed());
        }

        assertThat(runConcurrently(calls)).filteredOn(Boolean::booleanValue).hasSize(25);
    }

    @Test
    void test_cuc1_movingWindowFreesCapacityAsRequestsAgeOut() throws Exception {
        Policy policy = new Policy(901L, "two-per-second", RateLimitingStrategy.MOVING_WINDOW, 2,
                TimePeriod.SECOND, false);
        String key = "rl:window-consumer:resource:1:" + policy.fingerprint();

        assertThat(firstInstance.tryConsume(key, policy).allowed()).isTrue();
        assertThat(secondInstance.tryConsume(key, policy).allowed()).isTrue();
        RateLimitDecision rejected = firstInstance.tryConsume(key, policy);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter().toMillis()).isBetween(1L, 1000L);

        Thread.sleep(rejected.retryAfter().toMillis() + 50);
        assertThat(secondInstance.tryConsume(key, policy).allowed()).isTrue();
    }

    private static <T> List<T> runConcurrently(List<Callable<T>> calls) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<T> results = new ArrayList<>();
            for (Future<T> future : executor.invokeAll(calls)) {
                results.add(future.get());
            }
            return results;
        }
    }
}
