package com.example.ratelimiter.integration.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.adapter.out.cache.RuleCacheNames;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base class of all integration tests: full Spring context (one shared instance), MockMvc,
 * PostgreSQL and Redis containers, stub target API, and helpers for admin / consumer calls.
 * Redis counters and the rule cache are reset before each test because they are not covered by
 * database transaction rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    protected static final String SEED = "file:db/seed/seed.sql";
    protected static final String CLEANUP = "classpath:cleanup.sql";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected StringRedisTemplate redis;
    @Autowired
    protected CacheManager cacheManager;

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        Infrastructure.register(registry);
    }

    @BeforeEach
    void resetSharedState() {
        redis.execute((RedisCallback<Void>) IntegrationTest::flushAll);
        Objects.requireNonNull(cacheManager.getCache(RuleCacheNames.APPLICABLE_RULES)).clear();
        Infrastructure.TARGET_API.resetCounter();
    }

    protected ResultActions adminGet(String url, Object... variables) throws Exception {
        return mockMvc.perform(asAdmin(get(url, variables)));
    }

    protected ResultActions adminPost(String url, Object body) throws Exception {
        return mockMvc.perform(asAdmin(post(url)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions adminPut(String url, Object body) throws Exception {
        return mockMvc.perform(asAdmin(put(url)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions adminDelete(String url) throws Exception {
        return mockMvc.perform(asAdmin(delete(url)));
    }

    /** Creates an entity through the admin API and returns its id. */
    protected long createAndGetId(String url, Map<String, ?> body) throws Exception {
        String json = adminPost(url, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    protected String tokenFor(String consumerId) throws Exception {
        String json = adminPost("/admin/api/tokens", Map.of("consumer_id", consumerId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("access_token").asText();
    }

    protected ResultActions consumerCall(MockHttpServletRequestBuilder request, String token)
            throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    protected JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected long idOf(String sql, Object... args) {
        Long id = jdbc.queryForObject(sql, Long.class, args);
        return Objects.requireNonNull(id);
    }

    protected long consumerGroupId(String name) {
        return idOf("select id from consumer_groups where group_name = ?", name);
    }

    protected long resourceId(String method, String path) {
        return idOf("select id from resources where method = ? and resource_path = ?", method,
                path);
    }

    protected long resourceGroupId(String name) {
        return idOf("select id from resource_groups where name = ?", name);
    }

    protected long policyId(String name) {
        return idOf("select id from policies where name = ?", name);
    }

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic(Infrastructure.ADMIN_USERNAME,
                Infrastructure.ADMIN_PASSWORD));
    }

    private static Void flushAll(RedisConnection connection) {
        connection.serverCommands().flushAll();
        return null;
    }
}
