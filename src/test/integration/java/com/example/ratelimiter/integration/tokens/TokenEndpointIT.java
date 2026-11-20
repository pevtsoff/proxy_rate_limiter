package com.example.ratelimiter.integration.tokens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/** /admin/api/tokens: development tokens for registered consumers. */
@Transactional
@Sql(IntegrationTest.SEED)
class TokenEndpointIT extends IntegrationTest {

    @Test
    void test_cuc1_issuesBearerTokenAcceptedByTheProxy() throws Exception {
        adminPost("/admin/api/tokens", Map.of("consumer_id", "premium-user-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumer_id").value("premium-user-1"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.expires_at").isNotEmpty());

        consumerCall(get("/orders"), tokenFor("premium-user-1")).andExpect(status().isOk());
    }

    @Test
    void test_cuc1_refusesTokensForUnknownConsumers() throws Exception {
        adminPost("/admin/api/tokens", Map.of("consumer_id", "nobody"))
                .andExpect(status().isNotFound());
    }
}
