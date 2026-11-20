package com.example.ratelimiter.integration.policies;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/** MUC3 / MUC4: /admin/api/policies. Every test runs in a transaction that is rolled back. */
@Transactional
@Sql(IntegrationTest.SEED)
class PolicyEndpointIT extends IntegrationTest {

    private static final String POLICIES = "/admin/api/policies";

    @Test
    void test_muc4_createsReadsUpdatesAndDeletesPolicy() throws Exception {
        long id = createAndGetId(POLICIES, Map.of("name", "burst", "number_of_requests", 5,
                "time_period", "second"));

        adminGet(POLICIES + "/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("burst"))
                .andExpect(jsonPath("$.rate_limiting_strategy").value("moving-window"))
                .andExpect(jsonPath("$.number_of_requests").value(5))
                .andExpect(jsonPath("$.time_period").value("second"))
                .andExpect(jsonPath("$.default_policy").value(false));

        adminPut(POLICIES + "/" + id, Map.of("name", "burst", "rate_limiting_strategy",
                "token-bucket", "number_of_requests", 50, "time_period", "hour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate_limiting_strategy").value("token-bucket"))
                .andExpect(jsonPath("$.number_of_requests").value(50));

        adminDelete(POLICIES + "/" + id).andExpect(status().isNoContent());
        adminGet(POLICIES + "/{id}", id).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.https_status_code").value(404))
                .andExpect(jsonPath("$.resource").value("admin/api/policies/" + id));
    }

    @Test
    void test_muc4_rejectsInvalidPayloadsWithUniformError() throws Exception {
        adminPost(POLICIES, Map.of("name", "bad", "number_of_requests", 0, "time_period", "minute"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(startsWith("number_of_requests")))
                .andExpect(jsonPath("$.https_status_code").value(400));
        adminPost(POLICIES, Map.of("name", "bad", "rate_limiting_strategy", "leaky-bucket",
                "number_of_requests", 1, "time_period", "minute"))
                .andExpect(status().isBadRequest());
        adminPost(POLICIES, Map.of("name", "bad", "number_of_requests", 1, "time_period", "month"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test_muc4_rejectsDuplicateName() throws Exception {
        adminPost(POLICIES, Map.of("name", "free-10-per-minute", "number_of_requests", 1,
                "time_period", "minute"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.https_status_code").value(409));
    }

    @Test
    void test_muc4_listsWithFilteringAndPagination() throws Exception {
        adminGet(POLICIES + "?name=per-minute&page=0&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total_elements").value(5))
                .andExpect(jsonPath("$.total_pages").value(3));
        adminGet(POLICIES + "?strategy=token-bucket").andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(2));
    }

    @Test
    void test_muc4_exposesSupportedStrategies() throws Exception {
        adminGet(POLICIES + "/strategies").andExpect(status().isOk())
                .andExpect(jsonPath("$.supported_strategies",
                        hasItems("moving-window", "fixed-window", "token-bucket")))
                .andExpect(jsonPath("$.default_strategy").value("moving-window"))
                .andExpect(jsonPath("$.supported_time_periods",
                        hasItems("second", "minute", "hour", "day", "week")));
    }

    @Test
    void test_muc3_defaultPolicyIsPreCreatedAndCannotBeDeleted() throws Exception {
        String json = adminGet(POLICIES + "/default").andExpect(status().isOk())
                .andExpect(jsonPath("$.default_policy").value(true))
                .andExpect(jsonPath("$.rate_limiting_strategy").value("moving-window"))
                .andExpect(jsonPath("$.number_of_requests").value(60))
                .andExpect(jsonPath("$.time_period").value("minute"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(json).get("id").asLong();

        adminDelete(POLICIES + "/" + id).andExpect(status().isConflict());
    }

    @Test
    void test_muc4_assignedPolicyCannotBeDeleted() throws Exception {
        adminDelete(POLICIES + "/" + policyId("regular-100-per-minute"))
                .andExpect(status().isConflict());
    }

    @Test
    void test_muc4_requiresAdminCredentials() throws Exception {
        mockMvc.perform(get(POLICIES)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.https_status_code").value(401))
                .andExpect(jsonPath("$.resource").value("admin/api/policies"));
    }
}
