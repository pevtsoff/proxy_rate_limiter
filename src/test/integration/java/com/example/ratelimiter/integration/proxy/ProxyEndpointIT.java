package com.example.ratelimiter.integration.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.Infrastructure;
import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * CUC1: consumers call the proxy exactly like the target API. Seed: free tier has 10 reads per
 * minute on /orders (fixed-window), 3 writes per minute per write resource (token-bucket) and the
 * default policy (60/minute, moving-window) everywhere else.
 */
@Transactional
@Sql(IntegrationTest.SEED)
class ProxyEndpointIT extends IntegrationTest {

    @Test
    void test_cuc1_forwardsAllowedRequestWithVerifiedConsumerIdentity() throws Exception {
        consumerCall(get("/orders/1?expand=items"), tokenFor("free-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/orders/1"))
                .andExpect(jsonPath("$.query").value("expand=items"))
                .andExpect(jsonPath("$.consumer").value("free-user-1"))
                .andExpect(jsonPath("$.authorization").value("null"))
                .andExpect(header().string("X-Target", "stub"))
                .andExpect(header().string("X-RateLimit-Limit", "10"))
                .andExpect(header().string("X-RateLimit-Remaining", "9"))
                .andExpect(header().string("X-RateLimit-Strategy", "fixed-window"));
    }

    @Test
    void test_cuc1_forwardsRequestBodies() throws Exception {
        consumerCall(patch("/carts/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"qty\":3}"), tokenFor("premium-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("PATCH"))
                .andExpect(jsonPath("$.body_length").value(9));
    }

    @Test
    void test_cuc1_returns429WithMeaningfulJsonWhenCapacityIsExhausted() throws Exception {
        String token = tokenFor("free-user-1");
        for (int request = 1; request <= 10; request++) {
            consumerCall(get("/orders/1"), token).andExpect(status().isOk());
        }

        consumerCall(get("/orders/1"), token)
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"error": "You exceeded the request capacity for the current period",
                         "resource": "orders/1", "https_status_code": 429}""", true))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
        assertThat(Infrastructure.TARGET_API.receivedRequests()).isEqualTo(10);
    }

    @Test
    void test_cuc1_limitsArePerConsumer() throws Exception {
        String first = tokenFor("free-user-1");
        for (int request = 1; request <= 10; request++) {
            consumerCall(get("/orders"), first).andExpect(status().isOk());
        }
        consumerCall(get("/orders"), first).andExpect(status().isTooManyRequests());
        consumerCall(get("/orders"), tokenFor("free-user-2")).andExpect(status().isOk());
    }

    @Test
    void test_cuc1_writesOfFreeTierUseTokenBucket() throws Exception {
        String token = tokenFor("free-user-1");
        for (int request = 1; request <= 3; request++) {
            consumerCall(post("/orders"), token).andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Strategy", "token-bucket"));
        }
        consumerCall(post("/orders"), token).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.resource").value("orders"));
        consumerCall(get("/orders"), token).andExpect(status().isOk());
    }

    @Test
    void test_cuc1_resourceGroupPolicyIsSharedByAllResourcesOfTheGroup() throws Exception {
        adminPut("/admin/api/policies/" + policyId("regular-100-per-minute"), Map.of(
                "name", "regular-100-per-minute", "number_of_requests", 3,
                "time_period", "minute"));
        String token = tokenFor("regular-user-1");

        consumerCall(get("/orders"), token).andExpect(status().isOk());
        consumerCall(get("/orders/1"), token).andExpect(status().isOk());
        consumerCall(post("/orders"), token).andExpect(status().isOk());
        consumerCall(get("/orders/2"), token).andExpect(status().isTooManyRequests());
        consumerCall(get("/users/1"), token).andExpect(status().isOk());
    }

    @Test
    void test_muc3_defaultPolicyAppliesWhenNothingIsAssigned() throws Exception {
        consumerCall(get("/users/7"), tokenFor("free-user-1")).andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "60"))
                .andExpect(header().string("X-RateLimit-Policy", "default;w=60"))
                .andExpect(header().string("X-RateLimit-Strategy", "moving-window"));
        consumerCall(get("/not-in-openapi"), tokenFor("free-user-1")).andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Policy", "default;w=60"));
    }

    @Test
    void test_cuc1_rejectsMissingOrInvalidTokens() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resource").value("orders"))
                .andExpect(jsonPath("$.https_status_code").value(401))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));
        consumerCall(get("/orders"), "not-a-jwt").andExpect(status().isUnauthorized());
        assertThat(Infrastructure.TARGET_API.receivedRequests()).isZero();
    }

    @Test
    void test_cuc1_rejectsTokensOfUnregisteredConsumers() throws Exception {
        String token = tokenFor("free-user-1");
        jdbc.update("update consumers set consumer_id = 'renamed' "
                + "where consumer_id = 'free-user-1'");

        consumerCall(get("/orders"), token).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.https_status_code").value(403));
    }
}
